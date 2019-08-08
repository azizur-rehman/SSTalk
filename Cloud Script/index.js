const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp()
const NODE_TOKEN = 'FCM_Tokens';
const test_user_ID = 'Q7raoE5Jw7gWdx1JsxSbNd1kBgs1'; //emulator

const  FILE_TYPE_IMAGE = "image"
const  FILE_TYPE_LOCATION = "location"
const  FILE_TYPE_VIDEO = "video"

const MESSAGE_SEPERATOR = "<--MESSAGE_SEPERATOR-->";

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
 exports.testNotification = functions.https.onRequest((request, response) => {
  response.send("Hello from Firebase!");

  const payload = {
    notification: {
        title: 'Test notification',
        body: 'Test content',
        sound: "default",
        priority:"high",
    },
    data : {
        'target' : 'notificationTab'
    }
    
  }

  sendNotificationPayload(test_user_ID, payload)

 });


 //just a test function
exports.testHTTPFunction = functions.https.onRequest((request, response) => {
    response.send('Test HTTP function to check other functions')

    deleteOldImageFiles()
});


exports.triggerMessage = functions.database.ref('Message_Status/{UID}/{targetUID}/{messageID}/')
.onCreate((snapshot, context) => {

    var myUID = context.params.UID;
    var sender_uid = context.params.targetUID;
    var message_id = context.params.messageID;

   
    var from = snapshot.child('from').val();
    var isRead = snapshot.child('read').val();
    var senderPhone = snapshot.child('senderPhoneNumber').val();


    if(from === myUID || isRead === true)
        return false;


        //sender_uid -> myUID
        //message is sent to myUID from sender_uid



        console.log('--- Message from ---- '+sender_uid)
    
        console.log('my uid = '+myUID)
    
        var index = 0;
        var messages = '';

        var isNotificationSent = false;

        var isMuted = false


       return admin.database().ref('Mute_Notification')
        .child(myUID).child(sender_uid)
        .once('value', muteSnapshot => {
            if(muteSnapshot.exists()){
                if(muteSnapshot.child('enabled').val())
                    isMuted = true
            }


            return admin.database()
            .ref('Message_Status').child(myUID).child(sender_uid)
            .orderByChild('read').equalTo(false)
            .once('value', statusSnapshot => {
                var totalMessages = statusSnapshot.numChildren()
                var msgIDs = []
        
        
                console.log('total unread messages '+totalMessages)
        
                statusSnapshot.forEach(msg=>{
                    msgIDs.push(msg.key.toString())
        
                
                    if(isMuted){
                        //no need to fetch messages
                        return;
                    }
        
                    admin.database().ref('Messages')
                    .child(myUID)
                    .child(sender_uid)
                    .child(msg.key.toString())
                    .once('value', messageSnapshot => {
                        var type = messageSnapshot.child('messageType').val();
                        

                        var messageText = '';
        
                        if(type === FILE_TYPE_IMAGE){
                            messageText = '🖼 Image';
                        }
                        else if(type === FILE_TYPE_LOCATION){
                            messageText = '📌 Location';
                        }
        
                        else if(type === FILE_TYPE_VIDEO){
                            messageText = '🎥 Video';
                        }
                        else{
                            messageText = messageSnapshot.child('message').val();
                        }
        
                        //<-->
                        if(index <= 10)
                        messages = messages + messageText + MESSAGE_SEPERATOR;
        
                        if(index === totalMessages - 1){
        
                            console.log('Messages = '+messages);

                            var groupNameIfAny = msg.child('groupNameIfGroup').val()
                            console.log('Group name if any - '+groupNameIfAny)
        
        

                            if(groupNameIfAny === null)
                                groupNameIfAny = ''
          
                            const payload = {
                                data : {
                                    'unreadCount':totalMessages.toString(),
                                    'senderUID' : sender_uid,
                                    'receiverUID':myUID,
                                    'messageIDs': msgIDs.toString(),
                                    'senderPhoneNumber' :  senderPhone,
                                    'messages': messages,
                                    'groupNameIfAny':groupNameIfAny
                                }
                              }
                    
                              sendNotificationPayload(myUID, payload);
                        }
        
        
                    index++;
        
                    })
        
        
                })

                if(isMuted){
                    //sending mute notification with messageIDs in payload
                    const payload = {
                        data : {
                            'unreadCount':totalMessages.toString(),
                            'senderUID' : from,
                            'receiverUID':myUID,
                            'messageIDs': msgIDs.toString(),
                            'senderPhoneNumber' :  senderPhone
                        }
                      }
            
                      sendNotificationPayload(myUID, payload);
                }
        
            })

        })

    
  



})


exports.onNewFileUploaded = functions.storage.object().onFinalize(object => {

    var timeCreated = object.timeCreated
    var name = object.name


  

    console.log('---- FIle uploaded ----')

    console.log('Time = '+timeCreated)
    console.log('Name = '+name)

    cleanOldFiles();

});

exports.updateLastMessageNodeOnMessageDelete = functions.database.ref('Messages/{UID}/{targetUID}/{messageID}/')
.onDelete((snapshot, context) => {

    var myUID = context.params.UID;
    var target_uid = context.params.targetUID;
    var message_id = context.params.messageID;

    admin.database()
    .ref('Messages').child(myUID).child(target_uid)
    .once('value', msgSnapshot => {

        var timeInMillis = 0
        msgSnapshot.forEach(msg=>{
            timeInMillis = msg.child('timeInMillis').val()
        })

        if(timeInMillis !== 0){
            admin.database().ref('LastMessage').child(myUID).child(target_uid)
            .child('timeInMillis').set(timeInMillis)
            admin.database().ref('LastMessage').child(myUID).child(target_uid)
            .child('reverseTimeStamp').set(timeInMillis * -1)
            
        }
    })
})


//just a test function
exports.testHTTPFunction = functions.https.onRequest((request, response) => {
    response.send('Test HTTP function to check other functions')

    cleanOldFiles()
});

exports.onUserCreated = functions.auth.user().onCreate((user) => {
    return console.log(' --- User created ---\n'+JSON.stringify(user));
});

exports.onAppVersionUpdated = functions.database.ref('App_Version_Code').onWrite((snapshot, context) =>{
    var previousVersion = parseInt(snapshot.before.val())
    var newVersion = parseInt(snapshot.after.val())
    

    if(newVersion > previousVersion){

        console.log('Version Updated : '+previousVersion + ' -> '+newVersion)

        const payload = {
            notification : {
                title : 'SS Talk just got updated',
                body : 'A new update is available with lots of awesome features'
            }
        }

        return admin.database().ref('users')
        .once('value', snapshot => {
            snapshot.forEach(user => {
                 return sendNotificationPayload(user.key.toString(), payload);
            });
        });

       /* 
       return admin.messaging()
        //fZ6lFJyzvZs:APA91bH2ofK5mamu9vQHMO_PUbYGS8v7Ms4Exm2zHi7PrBVwYPV66ELRdrHQJfdxNwUCf8fRdtkVavsQVBinJWvz8EUywO13v27U2dIwewp9jpo8QdDrmeJadz-nmtTI--46FqYhQUsl
       // .sendToDevice('fZ6lFJyzvZs:APA91bH2ofK5mamu9vQHMO_PUbYGS8v7Ms4Exm2zHi7PrBVwYPV66ELRdrHQJfdxNwUCf8fRdtkVavsQVBinJWvz8EUywO13v27U2dIwewp9jpo8QdDrmeJadz-nmtTI--46FqYhQUsl',payload)
        .send(payload)
        .then(success => {
            return console.log('Notified users for new update')
            })
            .catch(error => {
               return console.log('Failed to send update notification = '+JSON.stringify(error)) 
         })
         */
        
    }
    else 
        return console.log('Version downgraded : '+newVersion +' --> '+cleanOldFiles)
});


function sendNotificationPayload(uid, payload){

    console.log('Sending notification to ---> '+uid)

    admin.database()
    .ref(NODE_TOKEN).child(uid)
    .once('value', snapshot=> {
        
        var tokens = []

        if(!snapshot.exists())
            return

        snapshot.forEach(item =>{
            tokens.push(item.val())
        });


        admin.messaging()
        .sendToDevice(tokens, payload)
        .then(res => {
            return console.log('Notification sent')
        })
        .catch(err => {
            return console.log('Error in sending notification = '+err)
        })

    })

}


function cleanOldFiles(){
  
  //getting currentTime in millis
  var currentTime  = new Date().getTime()

  var dayLimit = 30  //30 days

  var endLimit = currentTime - (dayLimit
     * 24
     * 60 
     * 60 * 1000)

     //delete old files
    console.log('--- Cleaning old files before '+endLimit+' ----')


  //fetching files stored before given limit
  admin.database()
  .ref('Files')
  .orderByChild('uploadTime')
  .endAt(endLimit)
  .once('value', fileSnapshot => {
      console.log('---> total files to delete = '+fileSnapshot.numChildren())
      fileSnapshot.forEach(function(file) {
        console.log('File data -> '+JSON.stringify(file.val()))
         
        deleteFile(file)
        //deleteFile(bucket, fileID, fileType, fileExtension)
      })
  })

}


function deleteFile(file){

    var fileID = file.child('fileID').val().toString()
    var fileType = file.child('fileType').val()
    var bucket = file.child('bucket_path').val()
    var fileExtension = file.child('file_extension').val()
  

  console.log('File to delete = '+bucket+fileType+'/'+fileID +fileExtension)

   var storage =  admin.storage()

    storage
    .bucket(bucket)
    .file(fileType+'/'+fileID)
    //+ fileExtension)
    .delete()
    .then(() => {

        admin.database()
        .ref('Files').child(fileID)
        .remove()   

       return console.log('file deleted with id - '+fileID)
    })
    .catch(err => {
        return console.log('Error in deletion : '+err.toString())
    })

    
}

