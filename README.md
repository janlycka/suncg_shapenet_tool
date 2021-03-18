# IsoSpace2Uima# suncg_shapenet_tool

Documentation
Kindly read the documentation in Text2Scene_AlexDayanaJan.pdf

Run:
Aufgabe 1:

a: place source XML in resources/spaceeval/XML/yourfile.xml
b: In intelliJ, right-click XMLtoJSON.java and run
c: take yourfile.json from resources/spaceeval and upload it to eschar to /mnt/rawindra/vol/studs/s7481099/deep-synth/deep-synth/shapenet
d: in putty, run python batch_synth_shapenet.py --save-dir aufgabe_2_tmp_data --data-dir living --model-dir res_1_living --location-epoch 300 --rotation-epoch 300 --start 0 --end 1 --shapenet-dir shapenet
e: wait for process to complete and locate *_final.json
f: copy *_final.json to resources/spaceeval/json/test/
g: In intelliJ, right-click Schritt12.java and run
e: the location of the new UIMA XML scene will be printed

Aufgabe 2:
a: in putty, run python batch_synth.py --save-dir aufgabe_2_tmp_data --data-dir bedroom --model-dir res_1_bedroom --start 0 --end 1 --rotation-epoch 180
b: wait for process to complete and locate *_final.json
c: copy *_final.json to resources/spaceeval/json/test/
d: in IntelliJ press alt+enter+f10, arrow to the right and click edit
e: set args as follows: -roomtype office -demo -nodel -path "./deep-synth/"
d: In intelliJ, right-click GenerateNewRoomWithDeepSynth.java and run
e: the location of the new UIMA XML scene will be printed
