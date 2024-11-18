# import tensorflow as tf
# import numpy as np
# import keras
import tensorflow_datasets as tfds
from tqdm import tqdm

import tensorflow as tf   

# Display the version
print(tf.__version__)     

# other imports
import numpy as np
import matplotlib.pyplot as plt
from tensorflow.keras.layers import Input, Conv2D, Dense, Flatten, Dropout
from tensorflow.keras.layers import GlobalMaxPooling2D, MaxPooling2D
from tensorflow.keras.layers import BatchNormalization
from tensorflow.keras.models import Model

BATCH_SIZE = 32


# Load in the data
cifar10 = tf.keras.datasets.cifar10

# Distribute it to train and test set
(x_train, y_train), (x_test, y_test) = cifar10.load_data()
print(x_train.shape, y_train.shape, x_test.shape, y_test.shape)

# Reduce pixel values
x_train, x_test = x_train / 255.0, x_test / 255.0

# flatten the label values
y_train, y_test = y_train.flatten(), y_test.flatten()

def normalize_img(image, label):
    """Normalizes images: `uint8` -> `float32`."""
    new_label = tf.one_hot(indices=label, depth=10)
    return tf.cast(image, tf.float32), tf.cast(new_label, tf.float32)

ds_train = tf.data.Dataset.from_tensor_slices((x_train[:1024], y_train[:1024]))
ds_train = ds_train.map(normalize_img)
ds_train = ds_train.batch(BATCH_SIZE)

# number of classes
K = len(set(y_train))
print("number of classes:", K)

def get_model():
    # Build the model using the functional API
    # input layer
    i = Input(shape=x_train[0].shape, batch_size=BATCH_SIZE)
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(i)
    x = BatchNormalization()(x)
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = MaxPooling2D((2, 2), )(x)

    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = MaxPooling2D((2, 2))(x)

    x = Conv2D(128, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(128, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = MaxPooling2D((2, 2))(x)

    x = Flatten()(x)
    x = Dropout(0.2)(x)

    # Hidden layer
    x = Dense(1024, activation='relu')(x)
    x = Dropout(0.2)(x)

    # last hidden layer i.e.. output layer
    x = Dense(K)(x)

    model = Model(i, x)
    
    return model


class MyModel(tf.Module):

  def __init__(self):
    self.model = get_model()
    self.model.summary()
    self.model.compile(
        optimizer=tf.keras.optimizers.Adam(0.001),
        loss=tf.keras.losses.CategoricalCrossentropy(from_logits=True))

  # The `train` function takes a batch of input images and labels.
  @tf.function(input_signature=[
      tf.TensorSpec([BATCH_SIZE] + list(x_train[0].shape), tf.float32),
      tf.TensorSpec([BATCH_SIZE, 10], tf.float32),
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
      tf.TensorSpec([BATCH_SIZE] + list(x_train[0].shape), tf.float32),
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
    model = MyModel()
    print()
    # print(model.model.layers[1].weights)
    SAVED_MODEL_DIR = "saved_model_cifar10"
    # model.compile(optimizer='sgd', loss='mean_squared_error') # compile the model

    x_all= None
    y_all = None
    curr_loss = float("inf")
    for x, y in (pbar := tqdm(ds_train, desc=f"Training: loss = {curr_loss}")):
        # breakpoint()
        if x_all is None:
            x_all = np.array(x)
            y_all = np.array(y)
        # print('Y SHAPADO:', y.shape, y.dtype)
        else:
            x_all = np.append(x_all, np.array(x), axis=0)
            # breakpoint()
            y_all = np.append(y_all, np.array(y), axis=0)
        curr_loss = model.train(x, y)['loss']
        pbar.set_description(f"Training: loss = {curr_loss}")

    print('\n\nX ALL SHAPADO!!!!!!!!!\n', x_all.shape)
    print('\n\nY ALL SHAPADO!!!!!!!!!\n', y_all.shape)
    
    x_all.tofile('cifar10_feats.bin')
    y_all.tofile('cifar10_labels.bin')

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

    with open("model_cifar10.tflite", 'wb') as f:
       f.write(tflite_model)

    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()

    infer = interpreter.get_signature_runner("infer")

    print("\nKERAS OUTPUT:\n", model.infer(x)['logits'][0])
    print("TFLITE OUTPUT:\n", infer(x=np.array(x).astype(np.float32))['logits'][0])

    train = interpreter.get_signature_runner("train")
    curr_loss = float("inf")
    
    for x, y in (pbar := tqdm(ds_train, desc=f"Training: loss = {curr_loss}")):
        # breakpoint()
        if x_all is None:
            x_all = np.array(x)
            y_all = np.array(y)
        # print('Y SHAPADO:', y.shape, y.dtype)
        else:
            x_all = np.append(x_all, np.array(x), axis=0)
            # breakpoint()
            y_all = np.append(y_all, np.array(y), axis=0)
        curr_loss = train(x=x, y=y)['loss']
        pbar.set_description(f"Training: loss = {curr_loss}")


if __name__ == "__main__":
    main()

