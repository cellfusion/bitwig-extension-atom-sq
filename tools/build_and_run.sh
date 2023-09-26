#/bin/sh

cd `dirname '$0'`

# Build the project
./gradlew build

# copy the jar to the Bitwig Extentions folder
cp ./build/libs/AtomSQ.bwextension ~/Documents/Bitwig\ Studio/Extensions/

# Run Bitwig
open /Applications/Bitwig\ Studio.app