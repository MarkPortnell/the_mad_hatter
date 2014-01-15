The Mad Hatter
----------

University of Edinburgh - Software Design Project 2013 - Group 6


Setting up your computer & Vision system
----------------------------------------

Clone the git archive to your Eclipse workspace ([Guide by Rimas](https://github.com/RimasTr/sdp-group6/wiki/Github-Quick-Start-Guide "Git Guide")).  
Download the Vision libraries (Thanks to Toms/Rimas) on Dropbox [here](https://www.dropbox.com/s/zvfgl8weytgp33l/visionlib.zip "Vision Libraries").  
Place the 'visionlib' folder in the the sdp-group6 folder.

Open terminal and type:
> gedit ~/.bashrc

Add the following lines, make sure to change *PathToYourWorkspaceFromHome* to your own path to your workspace.  
e.g. I do `export BASE=~/Eclipse/sdp-group6`
	
>export BASE=~/*PathToYourWorkspaceFromHome*/sdp-group6/  
>
>export PYTHONPATH=$PYTHONPATH:$BASE/visionlib/lib/python2.6/site-packages  
>export PYTHONPATH=$PYTHONPATH:$BASE/visionlib/opencv2.3/lib/python2.6/site-packages  
>export PYTHONPATH=$PYTHONPATH:$BASE/visionlib/sdl_font/lib  
>export PYTHONPATH=$PYTHONPATH:$BASE/visionlib/opencv2.3/lib  
>
>export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$BASE/visionlib/sdl_font/lib    
>export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$BASE/visionlib/lib/python2.6/site-packages  
>export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$BASE/visionlib/opencv2.3/lib  
>
>export NXJ_HOME=$BASE/lib/lejos_nxj  
>export PATH=$PATH:$NXJ_HOME/bin  
>export LOCALBASE=$LOCALBASE:$BASE/lib  
>export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$BASE/lib/libbluetooth  

Save it.  
Close it.

Open terminal and type:

> exec bash

You only have to do this once, the above file will be run every time you open the terminal, so now you can forget about it!

To test if it's working go to one of sdp-group-6 VISION PC's, open terminal and type:

> python $BASE/vision/vision.py -p0 -s

*Toms & Jack*

Setting up the java project
---------------------------

This project can be imported into Eclipse.  
There are several steps that must be taken before the code can compile and run successfully.

1/ The leJOS NXJ plugin must be installed. Instruction for doing this can be found [here](http://lejos.sourceforge.net/nxt/nxj/tutorial/Preliminaries/UsingEclipse.htm "Using Eclipse").

2/ Once you have installed the plugin, you must change a few settings.  
These are found in the 'Windows/Preferences' menu, under 'Lejos NXJ'. 
 
*  NXJ_HOME directory must be set to /*pathtoyourworkspace*/sdp-group6/lib/lejos_nxj
*  Under connection type, choose 'Bluetooth'
*  Check 'Connect to address' and enter: 00:16:53:08:A0:E6
*  Check 'Connect to named brick' and enter: group6

You may have to enter a code to pair the devices, the code is: `1111`.
	
3/ Setting up the libraries and dependencies.

* Right-click on the project folder in the Package Explorer (left hand side of Eclipse).
* Go to 'Build Path > Add libraries'	
* Now select 'LeJOS Library Container' and click 'Next'.
* Select the 'PC Libraries' and click finish.
* Now do the steps above againa and add 'NXT Libraries'.

If you get an error here, it may be because you already have the older libraries loaded.
Select go to 'Build Path > Configure Build Path'
Now remove the 'NXT' and 'PC' libraries if they are there, and any LeJOS container libraries.
The only libraries that should now be in your build path are:

* JRE System Library
* JUnit 4
* SDP (this should contain all the libraries that the rest of the code needs)

The code should now compile.

Again if not, make sure the SDP library contains the following libraries:

* Go to 'Build Path > Configure Build Path'
* Select 'SDP' then 'Edit' on the right hand side.
* Now select 'User Libaries'
* Click new and name the library 'SDP' then click 'Add JARs' on the right hand side.
* Now navigate to /sdp-group6/lib/ and all the .JAR files.
* Click 'OK', 'Finish' and then 'OK'.

Now it should be working!

4/
Now exit Eclipse.
Every time you open Eclipse, you must open it from the terminal.

> eclipse-4.2 &  
*The and symbol (&) will run the process in the background so you can continue to use the same terminal.*

5/ Eclipse is now running and all code should be working.
If not, please let me know asap!

*Check out the [Bluetooth](https://github.com/RimasTr/sdp-group6/blob/master/src/balle/bluetooth/README) and [Brick](https://github.com/RimasTr/sdp-group6/blob/master/src/balle/brick/README) READMEs too for extra information.*

*Jack*

