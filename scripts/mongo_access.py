#!/usr/bin/python
import sys,json,pprint
from distutils.sysconfig import get_python_lib
try:
    from bson.objectid import ObjectId
    from pymongo import MongoClient
except ImportError, e:
    print e
    print "Please make sure 'pip' or 'easy_install' are installed, and check the permissions on your python installation"
    print "After you have installed 'pip' or 'easy_install', run 'cd "+get_python_lib() +"; sudo chmod -R a+rX *; cd -'"
    sys.exit(-1)

args=sys.argv
mongo_host="badger-mdb1-1-crd.eng.org.net"

#converts Mongo cursor (query results) to a list of properties that can be loaded by Shell
def convertCursorToProperties(cursor):
    for record in cursor:
        print str(record)+'="'+str(cursor[record])+'"'

def usage():
    print "USAGE: mongo_access FINDONE|FIND|FINDBYID|POST|UPDATE|DELETE <collection_name> <json_doc> [optional <docId>]"

if len(args) < 4:
    usage()
    sys.exit(-1)


client = MongoClient('mongodb://' + mongo_host + ':27017/')
action = args [1]
db = client['badger-usage']
collection = db[args[2]]
doc = json.loads(args[3])
try:
 if len(args) == 5:
    docId = ObjectId(args[4])
except Exception, e:
    print e
    print "Error obtaining a record with id " +args[4]
    sys.exit(1)
if action == "POST":
    print collection.insert(doc)
elif action == "FIND":
    print collection.find(doc)
elif action == "FINDONE":
    print collection.find_one(doc)
elif action == "FINDBYID":
    convertCursorToProperties(collection.find_one({'_id': docId}))
elif action == "UPDATE":
    print collection.update({'_id': docId}, {"$set": doc}, upsert=False)
elif action == "DELETE":
    print collection.remove({'_id': docId})
else:
    raise ValueError("action "+action+" is not supported")


