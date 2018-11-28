import ffmpy

def compress_gif(gif_name):
    short_name = gif_name[:-1 * len(".gif")]
    out_name = short_name + ".mp4"
    ff_command = ffmpy.FFmpeg(
        inputs={gif_name: None},
        outputs={out_name: None}
    )
    ff_command.run()

def main():
    # gif_name = "out_RandomGraph30N100E6T1_B_g1_all.gif"
    gif_name = "out_SepLayerGraph0_noAnd_B_g1_all.gif"
    compress_gif(gif_name)

if __name__ == "__main__":
    main()
