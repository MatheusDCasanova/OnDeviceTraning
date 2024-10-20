import tensorflow as tf
import numpy as np
import keras
import tensorflow_datasets as tfds

IMG_SIZE = 28

def get_model():
    inputs = keras.Input(shape=[IMG_SIZE, IMG_SIZE, 1], dtype=tf.float32)
    # net = keras.layers.Lambda(lambda x : tf.expand_dims(x, axis=-1))(inputs)
    net = keras.layers.Conv2D(16, [3, 3])(inputs)
    net = keras.layers.Flatten()(net)
    net = keras.layers.Dense(128, activation='relu', name='dense_1', dtype=tf.float32)(net)
    out = keras.layers.Dense(10, name='dense_2', dtype=tf.float32)(net)

    return keras.models.Model(inputs, out)


def get_ds():
   (ds_train, ds_test), ds_info = tfds.load(
        'mnist',
        split=['train', 'test'],
        shuffle_files=True,
        as_supervised=True,
        with_info=True,
    )
   return ds_train, ds_test, ds_info

def get_processed_ds(ds_train, ds_info):

    def normalize_img(image, label):
        """Normalizes images: `uint8` -> `float32`."""
        new_label = tf.one_hot(indices=label, depth=10)
        return tf.cast(image, tf.float32) / 255., tf.cast(new_label, tf.float32)
   
    ds_train = ds_train.map(
        normalize_img, num_parallel_calls=tf.data.AUTOTUNE)
    # ds_train = ds_train.cache()
    ds_train = ds_train.shuffle(ds_info.splits['train'].num_examples)
    ds_train = ds_train.batch(128)
    # ds_train = ds_train.prefetch(tf.data.AUTOTUNE)

    return ds_train


class Model(tf.Module):

  def __init__(self):
    self.model = get_model()
    self.model.summary()

    self.model.compile(
        optimizer=tf.keras.optimizers.Adam(0.001),
        loss=tf.keras.losses.CategoricalCrossentropy(from_logits=True))

  # The `train` function takes a batch of input images and labels.
  @tf.function(input_signature=[
      tf.TensorSpec([None, IMG_SIZE, IMG_SIZE, 1], tf.float32),
      tf.TensorSpec([None, 10], tf.float32),
  ])
  def train(self, x, y):
    with tf.GradientTape() as tape:
      prediction = self.model(x)
      loss = self.model.loss(y, prediction)
    gradients = tape.gradient(loss, self.model.trainable_variables)
    self.model.optimizer.apply_gradients(
        zip(gradients, self.model.trainable_variables))
    result = {"loss": loss}
    return result

  @tf.function(input_signature=[
      tf.TensorSpec([None, IMG_SIZE, IMG_SIZE, 1], tf.float32),
  ])
  def infer(self, x):
    logits = self.model(x)
    probabilities = tf.nn.softmax(logits, axis=-1)
    return {
        "output": probabilities,
        "logits": logits
    }

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def save(self, checkpoint_path):
    tensor_names = [weight.name for weight in self.model.weights]
    tensors_to_save = [weight.read_value() for weight in self.model.weights]
    tf.raw_ops.Save(
        filename=checkpoint_path, tensor_names=tensor_names,
        data=tensors_to_save, name='save')
    return {
        "checkpoint_path": checkpoint_path
    }

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def restore(self, checkpoint_path):
    restored_tensors = {}
    for var in self.model.weights:
      restored = tf.raw_ops.Restore(
          file_pattern=checkpoint_path, tensor_name=var.name, dt=var.dtype,
          name='restore')
      var.assign(restored)
      restored_tensors[var.name] = restored
    return restored_tensors
  
def main():
    model = Model()
    print()
    # print(model.model.layers[1].weights)
    SAVED_MODEL_DIR = "saved_model"
    # model.compile(optimizer='sgd', loss='mean_squared_error') # compile the model

    train_ds, test_ds, ds_info = get_ds()
    train_ds = get_processed_ds(train_ds, ds_info)

    x_all= None
    y_all = None
    
    for x, y in train_ds:
        if x_all is None:
            x_all = x
            y_all = y
        # print('Y SHAPADO:', y.shape, y.dtype)
        else:
            x_all = np.append(x_all, x, axis=0)
            # breakpoint()
            y_all = np.append(y_all, y, axis=0)
            model.train(x, y)
    print('\n\nX ALL SHAPADO!!!!!!!!!\n', x_all.shape)
    x_all.tofile('mnist_feats.bin')
    y_all.tofile('mnist_labels.bin')
    # model.train(
    #    x=[
    #    [[-1, 0, 1, 2],
    #     [-2, 6, 9, 5]], 
    #    [[-2, 3, -2, 2],
    #     [8, -8, 1, 0]]
    #    ], 
    #    y=[[-3, -1, 1], [0, 1, 2]]) # train the model

    tf.saved_model.save(
        model,
        SAVED_MODEL_DIR,
        signatures={
            'train':
                model.train.get_concrete_function(),
            'infer':
                model.infer.get_concrete_function(),
            'save':
                model.save.get_concrete_function(),
            'restore':
                model.restore.get_concrete_function(),
        })

    # Convert the model
    converter = tf.lite.TFLiteConverter.from_saved_model(SAVED_MODEL_DIR)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable LiteRT ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    converter.allow_custom_ops = True
    converter.experimental_enable_resource_variables = True
    tflite_model = converter.convert()

    with open("model_mnist.tflite", 'wb') as f:
       f.write(tflite_model)

    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()

    infer = interpreter.get_signature_runner("infer")

    # print("\nKERAS OUTPUT:\n", model.infer(x)['logits'])
    # print("TFLITE OUTPUT:\n", infer(x=np.array(x).astype(np.float32))['logits'])


if __name__ == "__main__":
    main()