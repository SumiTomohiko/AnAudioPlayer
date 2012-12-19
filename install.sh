#Install the apk

. projectrc

apk="${app_name}_signed.apk"
path="/sdcard/${apk}"

#Remove the Old
rm -f "${path}"

#Only works if APK is on the sdcard
cp "${dist_dir}/${apk}" $(dirname "${path}")

#Now try and view it..
am start -a android.intent.action.VIEW -t application/vnd.android.package-archive -d file://${path}
