#!/bin/bash
# @author sarthur
# RPS package installer, used to install an RPS package on a VM.
# Invoke in the VM's puppet manifest with an entry like the following:
#
#       exec { "/vagrant/install-rps-pkg.sh -p zookeeper -r badger-zookeeper":
#         cwd => "/vagrant",
#         user => "vagrant"
#       }

function usage {
		echo "Usage: install-rps-pkg.sh -p <YD product> -r <YD role> -d <Base client dir>"
		echo "(Base client dir value can be found in the YD xml under the \"base_client_path\" option)"
		echo ""
		echo "e.g. ./install-rps-pkg.sh -p badger-zookeeper -r badger-zookeeper -d zookeeper"
		exit 1
}

# Parse args
while getopts "m:p:r:b:d:h:l:" opt
do
		case ${opt} in
			b)
					BRANCH=${OPTARG}
					;;
			d)
					BASE_CLIENT_DIR=${OPTARG}
					;;
			h)
					HOME=${OPTARG}
					;;
			l)
					LOG_DIR=${OPTARG}
					;;
			m)
					MANIFEST=${OPTARG}
					;;
			p)
					PRODUCT=${OPTARG}
					;;
			r)
					ROLE=${OPTARG}
					;;
			\?)
					usage
					;;
			*)
					usage
					;;
		esac
done

# Create the directory to hold install and manifest logs
mkdir -p ${LOG_DIR}

# When running inside Vagrant, Puppet's session will be root, so we have to set the home dir manually here.
export HOME=${HOME}
INSTALL_DIR=${HOME}/installed
RPS_STORE=https://<rps-store>

# Output from the install commands will go here
INSTALL_LOG=${LOG_DIR}/$( hostname )_${PRODUCT}_${ROLE}.log
echo "Logging to ${INSTALL_LOG}"
cat /dev/null > ${INSTALL_LOG}

# Get the filename which should hold the manifest for this VM. If it doesn't exist, reach out to RPS store and get latest
manifestFile="${LOG_DIR}/$( hostname )_${PRODUCT}_${ROLE}.manifest"

if [ -e ${manifestFile} ]
then
		manifestRmf=$( cat ${manifestFile} | sed 's/\s//g' )
		manifestName=$( echo ${manifestRmf} | cut -d . -f 1 )
		echo "Using checked in manifest rmf $manifestRmf  ($manifestName) from file $manifestFile to deploy"
fi

if [ ! -z ${MANIFEST} ]
then
		manifestRmf=${MANIFEST}
		manifestName=$( echo ${manifestRmf} | cut -d . -f 1 )
		echo "Using manifest rmf $manifestRmf  ($manifestName) to deploy"
fi

if [ -z $manifestName ]
then
		echo "Getting latest manifest from RPS store for product ${PRODUCT}, branch ${BRANCH}"
		wget -O ${manifestFile} --no-check-certificate "$RPS_STORE/GetMostRecentServlet?type=manifest&product=${PRODUCT}&releaseLabel=${BRANCH}" --certificate=utils/secure/org/org_ro_crt.pem --private-key=utils/secure/org/org_ro_key.pem 2>&1 >> ${INSTALL_LOG}
		manifestRmf=$( cat ${manifestFile} | sed 's/\s//g' )
		manifestName=$( echo ${manifestRmf} | cut -d . -f 1 )
		echo "Downloaded manifest rmf $manifestRmf ($manifestName) from RPS store $RPS_STORE"
		if [ -z $manifestName ]
		then
			echo "Could not determine a manifest to use"
			echo "Could not determine a manifest to use" >> ${INSTALL_LOG}
			exit 1
		fi
fi

echo "manifestName = [$manifestName]" >> ${INSTALL_LOG}

# Make sure we haven't already installed this version before proceeding. Makes this script idempotent
installedCount=$( ls -l ${INSTALL_DIR} 2>/dev/null | grep ${manifestName} | grep -v grep | wc -l )
if [ ${installedCount} -gt 0 ]
then
		echo "${PRODUCT} manifest ${manifestName} is already installed"
		echo "${PRODUCT} manifest ${manifestName} is already installed" >> ${INSTALL_LOG}
else
		echo "Installing ${PRODUCT} manifest ${manifestName}..." >> ${INSTALL_LOG}
		utils/pkg_installer.sh -b freeze -u $RPS_STORE -d test -r ${ROLE} -s internal -t runtime -m ${manifestRmf} -l DEFAULT 2>&1 >> ${INSTALL_LOG}
		echo "RETURN CODE IS $?"
fi

# Discover the full path of the installed product
echo "installed product path discovery for INSTALLDIR: [${INSTALL_DIR}]"
cd ${INSTALL_DIR}
dirName=$( ls -l | grep ${manifestName} | grep -v grep | awk '{print $9}' )
echo "dirName: [${dirName}]"
absDir=$( readlink -f ${dirName} )
echo "absDir: [${absDir}]"
productDir=${absDir}/${BASE_CLIENT_DIR}
echo "productDir: [${productDir}]"

# Munge user.properties to have the correct home.dir
cd ${productDir}/build
rm user.properties
echo "home.dir=${absDir}" >> user.properties

# Create symlink in home linking to this install
echo "removing /home/cloud-user/${BASE_CLIENT_DIR}"
rm -fr "/home/cloud-user/${BASE_CLIENT_DIR}"
ls -l /home/cloud-user
echo linking "/home/cloud-user/${BASE_CLIENT_DIR} --> ${productDir}"
ln -s ${productDir} /home/cloud-user/${BASE_CLIENT_DIR}
echo "exiting install-rps-pkg.sh"
exit 0
