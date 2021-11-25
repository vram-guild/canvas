readonly MC_VERSION="1.17"

echo "GRUNTLE REFRESH FOR $MC_VERSION - IF THIS IS NOT A 1.17 BRANCH YOU HAVE DONE A BAD"
echo 'Checking for build updates...'
# delete gruntle repo folder if exists from aborted run
if [ -d "gruntle-master" ]; then
  rm -rf gruntle-master
fi

# download and unpack latest gruntle bundle
curl https://github.com/vram-guild/gruntle/archive/refs/heads/master.zip -sSOJL
unzip -q gruntle-master

# copy content for our branch and then remove bundle
# this handles simple, file-based updates: checkstyle, standard gradle configs, etc.
cp -R gruntle-master/$MC_VERSION/ .
rm -rf gruntle-master
rm gruntle-master.zip

# run latest refresh
source gruntle/refresh.sh

# remove scripts
rm -rf gruntle

# following are not yet tested!

commit() {
    echo "Commiting and pushing update"
    git add *
    git commit -m "Gruntle automatic update"
    git push
}

if [[ $1 == 'auto' ]]; then
  commit
fi

if [[ $1 == "publish" ]]; then
  echo "Publishing jars"
  fabric/gradlew publish
fi

if [[ $1 == "build" ]]; then
  echo "Publishing jars"
  fabric/gradlew publish
  echo "Building and distributing"
  fabric/gradlew build --rerunTasks
  fabric/gradlew curseforge githubRelease publishModrinth
fi

echo 'Gruntle refresh complete'
