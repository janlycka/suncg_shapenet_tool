from scene_synth_shapenet import *
import argparse
import utils
"""
Sample script to call scene_synth
Modify as wanted
"""

from os import walk
def getFiles(folder, suncg=False):
    files = []
    tmpFiles = []
    for (dirpath, dirnames, filenames) in walk(folder):
        tmpFiles.extend(filenames)
        break
    for file in tmpFiles:
        if '.json' in file:
            files.append(file)
    if suncg:
        list1 = [int(x[:-5]) for x in files]
        list1.sort()
        files = [str(x)+'.json' for x in list1]
    return files

def getRoomSize(obj, bboxMin=False):
    minD, maxD = {'x':0.0,'y':0.0,'z':0.0}, {'x':0.0,'y':0.0,'z':0.0}
    nodecnt = 0
    for node in obj['levels'][0]['nodes']:
    
        if nodecnt > 0 and 'transform' in node:
            x,y,z = node['transform'][12], node['transform'][14], node['transform'][13]

            if minD['x'] > x or minD['x'] == 0:
                minD['x'] = x
            if minD['y'] > y or minD['y'] == 0:
                minD['y'] = y
            if minD['z'] > z or minD['z'] == 0:
                minD['z'] = z

            if maxD['x'] < x:
                maxD['x'] = x
            if maxD['y'] < y:
                maxD['y'] = y
            if maxD['z'] < z:
                maxD['z'] = z

        nodecnt+=1
    print("room size: " + str(maxD['x']-minD['x']) + " " + str(maxD['y']-minD['y']) + " " + str(maxD['z']-minD['z']) + " " + '\n')
    #print("room size: " + str(maxD['x']) + " " + str(maxD['y']) + " " + str(maxD['z']) + " " + '\n')
    if bboxMin:
        return {'x':minD['x'],'y':minD['y'],'z':minD['z']}
    return {'x':maxD['x']-minD['x'],'y':maxD['y']-minD['y'],'z':maxD['z']-minD['z']}

def getRoomSizeSUNCG(obj):
    pars = obj['levels'][0]['nodes'][0]
    return {'x':pars['width'],'y':pars['length'],'z':pars['height']}

def getRoomObjects(obj):
    bboxMin = getRoomSize(obj, bboxMin=True)

    objects = []
    #bboxMin = obj['levels'][0]['nodes'][0]['bbox']['min'];
    nodecnt = 0
    for node in obj['levels'][0]['nodes']:
        if nodecnt > 0 and 'transform' in node:
            node['transform'][12] -= bboxMin['x']
            node['transform'][14] -= bboxMin['y']
            node['transform'][13] -= bboxMin['z']
            objects.append(node)
        nodecnt+=1
    #print("room size: " + str(maxD['x']-minD['x']) + " " + str(maxD['y']-minD['y']) + " " + str(maxD['z']-minD['z']) + " " + '\n')
    #print("room size: " + str(maxD['x']) + " " + str(maxD['y']) + " " + str(maxD['z']) + " " + '\n')
    return objects



parser = argparse.ArgumentParser(description='Synth parameter search')
parser.add_argument('--temperature-cat', type=float, default=0.25, metavar='N')
parser.add_argument('--temperature-pixel', type=float, default=0.4, metavar='N')
parser.add_argument('--min-p', type=float, default=0.5, metavar='N')
parser.add_argument('--max-collision', type=float, default=-0.1, metavar='N')
parser.add_argument('--shapenet-dir', type=str, default="shapenet", metavar='S')
parser.add_argument('--save-dir', type=str, default="synth", metavar='S')
parser.add_argument('--data-dir', type=str, default="bedroom", metavar='S')
parser.add_argument('--model-dir', type=str, default="train/bedroom", metavar='S')
parser.add_argument('--continue-epoch', type=int, default=50, metavar='N')
parser.add_argument('--location-epoch', type=int, default=300, metavar='N')
parser.add_argument('--rotation-epoch', type=int, default=300, metavar='N')
parser.add_argument('--start', type=int, default=0, metavar='N')
parser.add_argument('--end', type=int, default=1, metavar='N')
parser.add_argument('--trials', type=int, default=1, metavar='N')
args = parser.parse_args()

#All the SceneSynth parameters that can be controlled 
params = {'temperature_cat' : args.temperature_cat,
          'temperature_pixel' : args.temperature_pixel,
          'min_p' : args.min_p,
          'max_collision' : args.max_collision}

print(params)
save_dir = args.save_dir
utils.ensuredir(save_dir)

s = SceneSynth(args.location_epoch, args.rotation_epoch, args.continue_epoch, args.data_dir, args.model_dir)

#get the best SUNCG-room-equivalent of the shapenet-room from SUNCG


import json

shapenetFiles = getFiles(args.shapenet_dir)
print('shapenetFiles', shapenetFiles)

idealRooms = []
for file in shapenetFiles:
    # read file
    print("reading: " + str(file))
    with open(args.shapenet_dir+'/'+file, 'r') as myfile:
        print("parsing: " + str(file))
        data=myfile.read()
        # parse file
        obj = json.loads(data)
        #stuff = {'nodes' : getRoomObjects(obj), 'size': getRoomSize(obj)}
        stuff = {'nodes' : getRoomObjects(obj), 'size': getRoomSize(obj)}

        idealRooms.append(stuff)

suncgFiles = getFiles('data/' + args.data_dir + '/json', suncg=True)
#print('suncg', 'data/' + args.data_dir + '/json', suncgFiles)

for room in idealRooms:
    size = room['size']
    maxSizeSoFar = {'x':0.0,'y':0.0,'z':0.0}
    results = []
    result = -1
    biggestRoomAvailable = ""
    obj = None
    for suncgFile in suncgFiles:
        with open('data/' + args.data_dir + '/json/'+suncgFile, 'r') as myfile:
            data=myfile.read()
            # parse file
            obj = json.loads(data)
            tmpSize = getRoomSize(obj)

            #print(suncgFile)
            #if suncgFile == '5.json':#if tmpSize['x'] >= size['x']:
            print('tmpSize', tmpSize, ' vs ', size)
            if tmpSize['x'] >= maxSizeSoFar['x'] and tmpSize['y'] >= maxSizeSoFar['y']:
                maxSizeSoFar['x'] = tmpSize['x'];
                maxSizeSoFar['y'] = tmpSize['y'];
                biggestRoomAvailable = suncgFile[:-5]
            if tmpSize['x'] >= size['x']+1 and tmpSize['y'] >= size['y']+1:# and tmpSize['z'] >= size['z']+1 and tmpSize['x'] >= size['y']+1 and tmpSize['y'] >= size['x']+1:
                size = tmpSize
                result = suncgFile[:-5]
                break
    if result == -1:
        results.append(biggestRoomAvailable)
    else:
        results.append(result)
    print('res', results)
    print('forcedNodes', room['nodes'])

    bbox = getRoomSize(obj)#obj['levels'][0]['nodes'][0]['bbox']

    paddingX = 55
    paddingY = 70
    paddingZ = 10
    
    for node in room['nodes']:

        node['transform'][12] /= bbox['x']
        node['transform'][14] /= bbox['y']
        node['transform'][13] /= bbox['z']

        node['transform'][12] *= (512-paddingX*2)
        node['transform'][14] *= (512-paddingY*2)
        node['transform'][13] *= (512-paddingZ*2)

        node['transform'][12] += paddingX
        node['transform'][14] += paddingY
        node['transform'][13] += paddingZ
    '''room['nodes'][0]['transform'] = [
            0.7071067811865474, \
            0.0, \
            0.7071067811865477, \
            0.0, \
            0.0, \
            0.9999999999999999, \
            0.0, \
            0.0, \
            -0.7071067811865477, \
            0.0, \
            0.7071067811865474, \
            0.0, \
            46.04932862149324, \
            -2.9288200198429593e-09, \
            27.781259160116317, \
            1.0]'''

    s.synth(results, trials=args.trials, save_dir=args.save_dir, **params, forcedNodes=room['nodes'], newRoomBBox=bbox)
    #s.synth(range(args.start, args.end), trials=args.trials, save_dir=args.save_dir, **params)