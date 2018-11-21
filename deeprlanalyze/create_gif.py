import json
import os.path
import imageio
from wand.image import Image
from wand.color import Color

# ImportError: MagickWand shared library not found.

def get_json_data(json_file):
    '''
    Loads the data from the file as Json into a new object.
    '''
    with open(json_file) as data_file:
        result = json.load(data_file)
        return result

def get_max_time(game_data):
    times = [x["time"] for x in game_data]
    if not times:
        return -1
    return max(times)

def get_game_data(status_data, game_index):
    games = status_data["games"]
    for game in games:
        cur_index = game["game_index"]
        if cur_index == game_index:
            return game["time_steps"]
    raise ValueError("Game not found: " + str(game_index))

def convert_image(pdf_name):
    print("to convert: " + pdf_name)
    with Image(filename=pdf_name, resolution=100) as img:
        with Image(width=img.width, height=img.height, background=Color("white")) as convert:
            convert.composite(img, 0, 0)
            new_name = pdf_name[:-1 * len(".pdf")] + ".png"
            convert.save(filename=new_name)

def make_gif(status_file_name, game_index):
    status_data = get_json_data(status_file_name)
    game_data = get_game_data(status_data, game_index)
    max_time = get_max_time(game_data)
    graph_name = status_data["graph_name"]
    short_graph_name = graph_name[:-1 * len(".json")]

    images = []
    for time_step in range(max_time + 1):
        pdf_name = "graph_figures/out_" + short_graph_name + "_g" + str(game_index) + \
            "_t" + str(time_step) + ".gv.pdf"
        image_name = pdf_name[:-1 * len(".pdf")] + ".png"
        if not os.path.isfile(image_name):
            convert_image(pdf_name)
        images.append(imageio.imread(image_name))
    output_name = "out_" + short_graph_name + "_g" + str(game_index) + "_all.gif"
    duration_per_frame = 0.5
    imageio.mimsave(output_name, images, duration=duration_per_frame)

def main():
    status_file_name = "test_game_result.json"
    game_index = 1
    make_gif(status_file_name, game_index)

'''
python create_gif.py
'''
if __name__ == "__main__":
    main()
