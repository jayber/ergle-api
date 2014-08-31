
db.events.find().forEach(function(doc){
    db.events.update({_id:doc._id}, {$push:{"to":doc.ownerEmail}});
});


db.events.update({}, {$unset:{"to":""}},{ multi: true });