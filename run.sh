#Script to start the Andoid app

. projectrc

pkg=$(echo ${pkg_dir} | sed -e s%/%.%g)

#'am' is found on most/all devices.. i hope..
am start -a android.intent.action.MAIN -n ${pkg}/.MainActivity
