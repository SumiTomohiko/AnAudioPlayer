#Build script

. projectrc

#Clean up
rm -rf "${build_dir}"
rm -rf "${dist_dir}"

#create the needed directories
mkdir_cmd="mkdir -m 770 -p"
${mkdir_cmd} "${dist_dir}"
${mkdir_cmd} "${build_dir}/classes"
${mkdir_cmd} "${libs_dir}"

#Rmove the R.java file as will be created by aapt
rm "${r_dir}/R.java"

#Now use aapt
echo Create the R.java file
aapt p -f -v -M AndroidManifest.xml -F "${build_dir}/resources.res" -I ~/system/classes/android.jar -S res/ -J "${r_dir}"

#cd into the src dir
cd "${src_dir}"

#Now compile - note the use of a seperate lib (in non-dex format!)
echo Compile the java code
cmd="javac ${javac_opt} -d ${classes_dir}"
widget_dir="jp/ddo/neko_daisuki/android/widget"
${cmd} "${widget_dir}/UzumakiDiagram.java"
jar="${libs_dir}/uzumaki.jar"
(cd ${classes_dir} && jar cf "${jar}" "${widget_dir}")

${cmd} -cp "${jar}" "${pkg_dir}/MainActivity.java"

#Back out
cd "${dirpath}"

#Now into build dir
cd "${classes_dir}"

#Now convert to dex format (need --no-strict) (Notice demolib.jar at the end - non-dex format)
echo Now convert to dex format
dx --dex --verbose --no-strict --output=${dex_path} $(echo ${pkg_dir} | cut -d / -f 1)

#Back out
cd "${dirpath}"

#And finally - create the .apk
apkbuilder "${dist_dir}/${app_name}.apk" -v -u -z ${build_dir}/resources.res -f ${dex_path}

#And now sign it
cd "${dist_dir}"
signer "${app_name}.apk" "${app_name}_signed.apk"

cd "${dirpath}"
