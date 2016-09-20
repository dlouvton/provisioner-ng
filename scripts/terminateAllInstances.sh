if [ -e ~/.vagrant/machines ]; then
   echo "Destroying the following running components (including statics):"
   rm -rf ~/.vagrant/machines/*/managed
   cd ~/rruc
   # grep the instance ids
   grep -H i-0 ~/.vagrant/machines/*/aws/id 2> /dev/null | tee instanceList.out
   while read line; do
      id=`echo $line | awk -F":" '{print $2}'`
      path=`echo $line | awk -F":" '{print $1}'`
      ! [[  $line =~ .*jenkins|badgerServer.* ]] && echo "Destroying $id" &&./rruc.rb EC2 TerminateInstances $id
      if [ $? -ne 0 ]; then echo "did not destroy $id"; continue; fi
      rm -rf $(dirname $(dirname $path))
   done < instanceList.out
else
   echo "No running instances exist, nothing to destroy"
fi
