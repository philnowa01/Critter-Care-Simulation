from PIL import Image, ImageSequence

gif = Image.open("C:\Users\philn\Desktop\Java_Test\Basic Asset Pack\MonkeyShake.gif")

frames = []

for frame in ImageSequence.Iterator(gif):
    frame = frame.convert("RGBA")

    data = frame.getdata()
    new_data = []

    for r, g, b, a in data:
        # Reinweiß entfernen
        if r == 255 and g == 255 and b == 255:
            new_data.append((255, 255, 255, 0))  # transparent
        else:
            new_data.append((r, g, b, a))

    frame.putdata(new_data)
    frames.append(frame)

frames[0].save(
    "output.gif",
    save_all=True,
    append_images=frames[1:],
    loop=gif.info.get("loop", 0),
    duration=gif.info.get("duration", 100),
    disposal=2
)