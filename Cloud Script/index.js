const functions = require('firebase-functions');
const admin = require('firebase-admin');
const phoneRegEx = require('phone-regex');
admin.initializeApp();

const info_vehicle = 'info_vehicle';

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

const actionCodeSettings = {
  // URL you want to redirect back to. The domain (www.example.com) for
  // this URL must be whitelisted in the Firebase Console.
  iOS: {
    bundleId: 'com.example.ios'
  },
  android: {
    packageName: 'com.mimansaa.trackmychild',
    installApp: true,
    minimumVersion: '16'
  }
};

exports.testHTTPFunction = functions.https.onRequest((req, res) => {

  var actionCodeSettings = {
    url: 'https://www.example.com/cart?email=user@example.com&cartId=123',
    iOS: {
      bundleId: 'com.example.ios'
    },
    android: {
      packageName: 'com.example.android',
      installApp: true,
      minimumVersion: '12'
    },
    handleCodeInApp: true,
    dynamicLinkDomain: 'custom.page.link'
  };

  admin.auth()
    .generateEmailVerificationLink('azizur.rehman007@gmail.com', actionCodeSettings)
    .then(function(link) {
      // The link was successfully generated.
      console.log('Verificatio mail sent')
      return res.status(200).send(link)
    })
    .catch(function(error) {
      // Some error occurred, you can inspect the code: error.code
      console.log('Mail not sent')
      return res.status(400).send(error)

    });
});                    

exports.testHTTPFunction2 = functions.https.onRequest((req, res) => {
                     // sdd = space, dot, or dash
   // var pattern =  "(\\+[0-9]+[\\- \\.]*)?(\\([0-9]+\\)[\\- \\.]*)?([0-9][0-9\\- \\.]+[0-9])"
    var phone = req.body.phone
    //var regex = new RegExp(pattern)
    
   var isValid = isPhoneValid(phone)
  res.status(200).json(phone + ' is valid = '+isValid)

});

exports.createUser = functions.https.onRequest((request, response) => {

    var body = request.body.text
    var email = request.body.email
    var password = request.body.password
    var displayName = request.body.name
    var phone = request.body.phone
    var isEmailVerified = request.body.isEmailVerified === '1'


    console.log('----- Creating user ------')
    console.log(JSON.stringify(request.body))

    admin.auth().createUser({
        email: email,
        phoneNumber:phone,
        emailVerified: isEmailVerified,
        password: password,
        displayName: displayName,
        disabled: false
      }).then(function(user) {
  
       // if(!isEmailVerified)
         // admin.auth().sendVerificationEmail()

        return response.set('Access-Control-Allow-Origin', '*').status(200).send(JSON.stringify(user))
      })
      .catch(function(error){
        console.log(error)
        return response.set('Access-Control-Allow-Origin', '*').status(400).send(JSON.stringify(error))
      })


});

exports.updateUserEmail =  functions.https.onRequest((request, response) => {
  console.log('----- Updating user email ----')


  var uid = request.body.uid
  var emailID = request.body.email


  if(uid === undefined){
    return response.set('Access-Control-Allow-Origin', '*').status(401).send(JSON.stringify({error : 'UID Missing'}))
  }



  console.log('uid = '+uid +', email = '+emailID)


  admin.auth().updateUser(uid, {
    email : emailID,
    emailVerified: false
  })
  .then(user => {
    admin.database().ref('all_users').child(uid).child('email').set(emailID)
    return response.set('Access-Control-Allow-Origin', '*').status(200).send(JSON.stringify(user))
  })
  .catch(err => {
    return response.set('Access-Control-Allow-Origin', '*').status(400).send(JSON.stringify({error : err}))
  })


});



exports.updateUserPhone =  functions.https.onRequest((request, response) => {


  console.log('----- Updating user phone ----')

  var uid = request.body.uid
  var phone = request.body.phone


  if(uid === undefined){
    return response.set('Access-Control-Allow-Origin', '*').status(401).json({
      error : 'UID Missing'
    })
  }


  console.log('uid = '+uid +', email = '+phone)

  admin.auth().updateUser(uid, {
    phoneNumber : phone
  })
  .then(user => {
    admin.database().ref('all_users').child(uid).child('phone').set(phone)
    return response.set('Access-Control-Allow-Origin', '*').status(200).json({user})
  })
  .catch(err => {
    return response.set('Access-Control-Allow-Origin', '*').status(400).json({
      error : err
    })
  })


});



exports.updateUserDetail =  functions.https.onRequest((request, response) => {
  var UserUID = request.body.uid
  var phone = request.body.phone
  var email = request.body.email
  var displayName = request.body.name

  var authProperty;

  console.log('----- Updating user detail ----')
  console.log('uid = '+UserUID +', email = '+email)
  console.log('phone = '+phone +', displayName = '+displayName)



  if(UserUID === undefined)
    return response.set('Access-Control-Allow-Origin', '*').status(400).send(JSON.stringify({error : "UID missing"}))
  

    // for both email and phone
  if(email !== undefined && phone !== undefined){
    authProperty = {
      phoneNumber : phone,
      email : email,
      displayName : displayName
    }
  }

  //for phone only
  else if(phone!==undefined)
   authProperty = {
    phoneNumber : phone,
    displayName : displayName
  }

  // for email only
  else if(email!==undefined)
   authProperty = {
    email : email,
    displayName : displayName
  }


  admin.auth().updateUser(UserUID, authProperty).then(function(user){
    return response.set('Access-Control-Allow-Origin', '*').status(200).send(JSON.stringify(user))
  })
  .catch(function(error){
    console.log('error in updating phone = '+error)
    return response.set('Access-Control-Allow-Origin', '*').status(400).send(JSON.stringify(error))
  })

});


exports.onChildAssigned = functions.database.ref('vehicle_assigned_child/{vehicle_id}/')
.onWrite((snapshot, context) => {

  const vehicleID = context.params.vehicle_id

  admin.database().ref('vehicle_assigned_child')
  .child(vehicleID)
  .once('value', datasnapshot => {
    const count = datasnapshot.numChildren()
    return snapshot.ref.child('occupied_seats').set(count)
  })

});

function isPhoneValid(phone){
  return phoneRegEx({exact:true}).test(phone)
}


exports.onChildInfoChanged = functions.database.ref('info_child/{child_id}/')
.onUpdate((snapshot, context) => {
  const oldVehicleID =  snapshot.before.child('currentVehicleID').val()
  const newVehicleID =  snapshot.after.child('currentVehicleID').val()
  
  console.log(JSON.stringify(snapshot.before.val()) + ' was changed' )

  // Vehicle assigned for the first time
  if(oldVehicleID === '' && newVehicleID !== ''){
    const parentUID = snapshot.before.child('parentUID').val()
    const childID = snapshot.before.child('id').val()

    if(parentUID !== '' && childID !== ''){
      const subscription = Date.now() + (30 * 24 * 60 * 60 * 1000)
      admin.database().ref('parent_child').child(parentUID).child(childID).child('subscriptionEnd')
        .set(subscription).then(success => {
          return console.log('Vehicle has been assigned and 30 days free subscription has been awarded')
        })
        .catch(error => {
          var err = JSON.stringify(error)
          return console.log(JSON.stringify({message : 'Something went wrong while assigning free subscription',
          data : 'parent uid = '+parentUID+', childID = '+childID,
          error: err
        }))
        })
    }
    else{
      return console.log('Detail missing, parent uid = '+parentUID+', childID = '+childID )
    }

  }
    

});
