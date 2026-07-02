from PIL import Image, ImageSequence
import numpy as np

gif = Image.open(r"C:\Users\philn\Desktop\ZZZZ\pytestttt\MonkeyShake.gif")
frames = []


# Diddler help

for frame in ImageSequence.Iterator(gif):
    arr = np.array(frame.convert("RGBA"))

    mask = (
        (arr[:, :, 0] > 230) &
        (arr[:, :, 1] > 230) &
        (arr[:, :, 2] > 230)
    )

    arr[mask, 3] = 0  # Alpha = transparent

    frames.append(Image.fromarray(arr))

frames[0].save(
    "output.gif",
    save_all=True,
    append_images=frames[1:],
    loop=gif.info.get("loop", 0),
    duration=gif.info.get("duration", 100),
    disposal=2
)