JAR_EXECUTABLE = puzzlegames.jar
JAR_MAINCLASS = GUI
JAR_CONTENTS = $(SOURCE_FILES) $(CLASS_FILES) $(CONFIG_FILES) $(RESOURCE_FILES)
CLASS_FILES = *.class hulka/event/*.class hulka/gui/HTMLDialog.class hulka/gui/HTMLDialogHandler.class hulka/graphics/JBufferedCanvas.class hulka/util/JVMVersion.class hulka/util/MiscUtils.class hulka/gui/ImageListCellRenderer.class hulka/gui/JSimpleImageCombo.class hulka/gui/UnselectedListCellRenderer.class

SOURCE_FILES_GUI = hulka/gui/HTMLDialog.java hulka/gui/HTMLDialogHandler.java hulka/gui/ImageListCellRenderer.java hulka/gui/JSimpleImageCombo.java hulka/gui/UnselectedListCellRenderer.java
CLASS_FILES_GUI = hulka/gui/HTMLDialog.class hulka/gui/HTMLDialogHandler.class hulka/gui/ImageListCellRenderer.class hulka/gui/JSimpleImageCombo.class hulka/gui/UnselectedListCellRenderer.class
SOURCE_FILES_EVENT = hulka/event/*.java
CLASS_FILES_EVENT = hulka/event/*.class
SOURCE_FILES_GUI = hulka/gui/*.java
CLASS_FILES_GUI = hulka/gui/*.class
SOURCE_FILES_UTIL = hulka/util/ImageMap.java hulka/util/JVMVersion.java hulka/util/MiscUtils.java
CLASS_FILES_UTIL = hulka/util/ImageMap.class hulka/util/JVMVersion.class hulka/util/MiscUtils.class
SOURCE_FILES_TILE = hulka/tilemanager/AbstractTileManagerImpl.java hulka/tilemanager/HexJigsawManager.java hulka/tilemanager/HexSpinnerManager.java hulka/tilemanager/HexTileManager.java hulka/tilemanager/SquareJigsawManager.java hulka/tilemanager/SquareTileManager.java hulka/tilemanager/TileManager.java hulka/tilemanager/TileSetDescriptor.java hulka/tilemanager/TileSpinnerManager.java
CLASS_FILES_TILE = hulka/tilemanager/AbstractTileManagerImpl.class hulka/tilemanager/HexJigsawManager.class hulka/tilemanager/HexSpinnerManager.class hulka/tilemanager/HexTileManager.class hulka/tilemanager/SquareJigsawManager.class hulka/tilemanager/SquareTileManager.class hulka/tilemanager/TileManager.class hulka/tilemanager/TileSetDescriptor.class hulka/tilemanager/TileSpinnerManager.class
SOURCE_FILES_XML = hulka/xml/SimpleXMLReader.java hulka/xml/SimpleXMLToken.java hulka/xml/SimpleXMLMatcherFactory.java hulka/xml/SimpleXMLEncoder.java
CLASS_FILES_XML = hulka/xml/SimpleXMLReader.class hulka/xml/SimpleXMLToken.class hulka/xml/SimpleXMLMatcherFactory.class hulka/xml/SimpleXMLEncoder.class

SOURCE_FILES_GAME = *.java
CLASS_FILES_GAME = *.class

CONFIG_FILES = *.xml makefile
RESOURCE_FILES = images/* pics/* pics/thumbs/* license/* credits/* help/*

SOURCE_FILES = $(SOURCE_FILES_GUI) $(SOURCE_FILES_EVENT) $(SOURCE_FILES_GRAPHICS) $(SOURCE_FILES_UTIL) $(SOURCE_FILES_TILE) $(SOURCE_FILES_XML) $(SOURCE_FILES_GAME)
CLASS_FILES = $(CLASS_FILES_GUI) $(CLASS_FILES_EVENT) $(CLASS_FILES_GRAPHICS) $(CLASS_FILES_UTIL) $(CLASS_FILES_TILE) $(CLASS_FILES_XML) $(CLASS_FILES_GAME)

SOURCE_VM = 1.5
TARGET_VM = 1.5

clean : 
	touch $(SOURCE_FILES)
#compiles the source files
compile : $(CLASS_FILES)
#builds everything needed to run the application as an executable jar
all : $(JAR_EXECUTABLE)

$(JAR_EXECUTABLE) : $(JAR_CONTENTS)
	jar -cvfe $(JAR_EXECUTABLE) $(JAR_MAINCLASS) $(JAR_CONTENTS)

$(CLASS_FILES) : $(SOURCE_FILES)
	javac -source $(SOURCE_VM) -target $(TARGET_VM) $(SOURCE_FILES)
