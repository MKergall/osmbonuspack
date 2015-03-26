# Prerequisites #
You should be familiar with Android SDK.

In the explanations, we assume that you are using Eclipse, and that you have an Android project created.

# First steps #
First of all, you must include osmdroid library in your Android project.
Refer to [osmdroid](https://github.com/osmdroid/osmdroid) documentation.

OSMBonusPack jars from v5.1 are based on osmdroid v4.3

(OSMBonusPack jars from v4.5 to v5.0 are based on osmdroid v4.2;
OSMBonusPack jars from v4.2.6 to v4.4 are based on osmdroid v4.1)

Take care to:
  * put the osmdroid.jar in a "libs" directory
  * configure the Build Path of your project so that osmdroid.jar appears in the "Referenced Libraries"
  * set-up the permissions required by osmdroid in your AndroidManifest.xml (OSMBonusPack doesn't requires anything more than osmdroid).

Done? OK. So, there are 2 ways to include OSMBonusPack, each one with pros and cons.


# 1. Simple way - Including osmbonuspack.jar #
[Download](http://code.google.com/p/osmbonuspack/source/browse/#svn%2FBonusPackDownloads) the latest version of osmbonuspack.jar

Copy it in the "libs" directory of your project, with osmdroid.jar.

Right-click on the project, "Build Path", "Configure Build Path...", "Add JARs...".

Select osmbonuspack.jar in libs, and click OK.
It's added to the "Referenced Libraries", ready-to-use.

Following exactly the same method, you will also have to add the following libs:
  * The Google-gson parser: [gson-2.2.4.jar](https://code.google.com/p/google-gson/downloads/list)
  * The Apache Commons Lang: [commons-lang3-3.3.2.jar](http://commons.apache.org/proper/commons-lang/)

You can get them [here](https://code.google.com/p/osmbonuspack/source/browse/#svn%2Ftrunk%2FOSMBonusPack%2Flibs).

If you are going to use standard OSMBonusPack "bubbles", browse the source, go to trunk/OSMNavigator and download in your project resources the following files:
  * For a white bubble:
    * drawable-mpi/bonuspack\_bubble.9.png
    * layout/bonuspack\_bubble.xml
  * For a dark-grey bubble:
    * drawable-mpi/bonuspack\_bubble\_black.9.png
    * layout/bonuspack\_bubble\_black.xml
  * For the "more info" button:
    * drawable/btn\_moreinfo.xml
    * drawable-mpi/moreinfo\_arrow.png
    * drawable-mpi/moreinfo\_arrow\_pressed.png

# 2. Set-up OSMBonusPack as an Android library project #
Checkout OSMBonusPack project from the SVN repository and set it up as an "Android library" project in your Eclipse workspace.
Then, in the properties of your application project, in the Android section, add OSMBonusPack as a library.

Then download needed files in your project resources, as described in Chapter 1 above.