int maxNeighbors = 8;


User loadFLICKRuser(String handle){  
  User newUser = null;
  if(userAlreadyExists(handle)){
    printLog("user " + handle + " is already loaded");
    printLog("going to try and load friends for this user");    
    
    return null;
  }
  XMLInOut     xmlLoader   = new XMLInOut(this);
  XMLElement   flickrUser;
  
  //first we try and get the NSID from the username
  String loadURL = FLICKRgetNSID + formatURL(handle);
  flickrUser = xmlLoader.loadElementFrom( loadURL );
  println(loadURL);

  //check if user or xml is valid before continuing
  String loadStatus = flickrUser.getAttribute("stat");
  
  //if we get a "fail" or anything else, don't load this user, it is fail
  if(!loadStatus.equals("ok"))
    return null;
   
  //continue with the load
  //get the nsid  
  flickrUser = flickrUser.firstChild();  
  String nsid = flickrUser.getAttribute("nsid");
  
  //now get the actual username
  flickrUser = flickrUser.firstChild();
  String username = flickrUser.firstChild().getText();
  
  //notify that we are loading
  printLog("loading Flickr user: " + username);    

  //String iconServer = flickrUser.getAttribute("iconserver");
  
  //create new user from this information
  newUser = new User();
  newUser.contactFLICKR = handle;  
  newUser.contactFLICKRNSID = nsid;
  
  //load this users friends
  XMLElement friends = null;;        
  try{
    printLog("loading flickr user friends of " + formatURL(newUser.contactFLICKR) + " NSID...");
    friends = xmlLoader.loadElementFrom(FLICKRgetFriends + formatURL(newUser.contactFLICKRNSID) );
    int numFriends = 0;
    if(friends.hasChildren()){
      XMLElement contacts = friends.getChild(0);
      numFriends = contacts.getIntAttribute("total");
      totalFlickrFriendsExpected+=numFriends;
    }
    //println(newUser.handle + " has " + numFriends + " flickr friends");
    //println("friends url: " + FLICKRgetFriends + newUser.contactFLICKRNSID);
  }catch(Exception e){
    printLog("flickr user friends of " + newUser.contactFLICKR + " has failed to load!");
    println("error with url: " + FLICKRgetFriends + formatURL(newUser.contactFLICKRNSID));
    return null;
  }
  newUser.setFlickrFriends(friends);    
  
  //the rest of the data will be retrieved by user method getFlickrData()
  
  /*    
  newUser.contactFLICKRiconServer = iconServer;
  newUser.formatText();
  
  //flickr user needs thumbnails too!
  newUser.hasNoThumbnail();  
  */  
  
  //add this user to the master user list
  if(newUser != null)
    users.add( newUser );
  
  //add the new user as an egg cell
  createCellFromUser( newUser );    
  
  if(newUser!=null)
    return newUser;
  
  //failiure to load...    
  return null;  
}

User loadLASTFMuser(String handle){  
  User newUser = null;
  if(userAlreadyExists(handle)){
    printLog("user " + handle + " is already loaded");
    printLog("going to try and load friends for this user");    
    
    return null;
  }
  XMLInOut     xmlLoader   = new XMLInOut(this);
  
  //do a check here to see if xml file exists
   
  //do a check for the actual username (catch casing and formatting differences)    
  
  //notify that we are loading
  printLog("loading lastFM user: " + handle);    

  //create new user from this information
  newUser = new User();
  newUser.contactLASTFM = handle;  
  newUser.gearonUser = true;
  

  //load this users friends
  XMLElement friends;    
  try{
    printLog("loading lastFM friends list of " + newUser.contactLASTFM);
    friends = xmlLoader.loadElementFrom(LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");
          
    //println(LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");
    //println("loading lastfm user: "+contactLASTFM);
  }catch(Exception e){
    //if the xml file could not be loaded it has to be created
    printLog("the friends list from lastfm user failed to load!");
    println("error with url: " + LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");          
    return null;
  }     
  newUser.setLastfmFriends(friends);    
  
  //add this user to the master user list
  if(newUser !=null)
    users.add( newUser );
  
  //add the new user as an egg cell
  createCellFromUser( newUser );    
  
  if(newUser!=null)
    return newUser;
  
  //failiure to load...    
  return null;  
}

User loadGOuser(String handle){  
  XMLInOut     xmlLoader   = new XMLInOut(this);
  
  //do a check here to see if xml file exists
  XMLElement xml = null;
  try{
    xml = xmlLoader.loadElementFrom(GOserverURL + handle);
  }
  catch(Exception e){
    printLog(""+e);
    printLog("something went wrong with loading Fidgt URL");
    return null;
  }
   
  //do a check for the actual username (catch casing and formatting differences)    
  if( xml.countChildren() <=1 ){
    printLog("user " + handle + " not found");
    return null;
  }
  
  //create new user
  User newUser = null;  
  newUser = new User();
  
  XMLElement elements[] = xml.getChildren();
  
  XMLElement handles[] = elements[0].getChildren();
  for(int i=0; i<handles.length; i++){
    String type = handles[i].getName();
    if(type.equals("aim")){
      continue;
    }
    
    
    if(type.equals("lastfm")){
      newUser.contactLASTFM = handles[i].firstChild().getText();
      
      //load this users friends
      XMLElement friends;    
      try{
        printLog("loading lastFM friends list of " + newUser.contactLASTFM);
        friends = xmlLoader.loadElementFrom(LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");
          
        //println(LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");
        //println("loading lastfm user: "+contactLASTFM);
      }catch(Exception e){
        //if the xml file could not be loaded it has to be created
        printLog("the friends list from lastfm user failed to load!");
        println("error with url: " + LASTFMserverURL + formatURL(newUser.contactLASTFM) + "/friends.xml");          
        return null;
      }     
      newUser.setLastfmFriends(friends);      
      
      continue;
    }
    
    
    if(type.equals("flickr")){
      newUser.contactFLICKR = handles[i].firstChild().getText();
      
      //  not done yet!! we need the NSID as well
      //first we try and get the NSID from the username
      String loadURL = FLICKRgetNSID + formatURL(newUser.contactFLICKR);
      XMLElement flickrUser;
      flickrUser = xmlLoader.loadElementFrom( loadURL );

      //check if user or xml is valid before continuing
      String loadStatus = flickrUser.getAttribute("stat");
  
      //if we get a "fail" or anything else, then something's wrong with the flickr data
      if(!loadStatus.equals("ok")){
        newUser.contactFLICKR = null;
        continue;
      }
   
      //continue with the load
      //get the nsid  
      flickrUser = flickrUser.firstChild();  
      String nsid = flickrUser.getAttribute("nsid");
      
      newUser.contactFLICKRNSID = nsid;      
      
      //load this users friends
      XMLElement friends = null;;        
      try{
        printLog("loading flickr user friends of " + newUser.contactFLICKR);
        friends = xmlLoader.loadElementFrom(FLICKRgetFriends + formatURL(newUser.contactFLICKRNSID) );
        int numFriends = 0;
        if(friends.hasChildren()){
          XMLElement contacts = friends.getChild(0);
          numFriends = contacts.getIntAttribute("total");
          totalFlickrFriendsExpected+=numFriends;
        }
        //println(newUser.handle + " has " + numFriends + " flickr friends");
        //println("friends url: " + FLICKRgetFriends + newUser.contactFLICKRNSID);
      }catch(Exception e){
        printLog("flickr user friends of " + newUser.contactFLICKR + " has failed to load!");
        println("error with url: " + FLICKRgetFriends + formatURL(newUser.contactFLICKRNSID));
        return null;
      }
      newUser.setFlickrFriends(friends);          
      
      continue;
    }
    if(type.equals("jabber")){
      continue;
    }
  }
  

  XMLElement currentHandleElement = elements[1];  
  String currentHandle = currentHandleElement.firstChild().getText();
  if(userAlreadyExists(currentHandle)){
    printLog("user " + handle + " is already loaded");
    printLog("going to try and load friends for this user");        
    return null;
  }  
  else{
    newUser.handle = currentHandle;
  }
  
  
  //notify that we are loading     
  XMLElement currentThumbnail = elements[2];  
  if(currentThumbnail.hasChildren()){
    newUser.thumbnailURL = currentThumbnail.firstChild().getText();
    newUser.hasNoThumbnail();  
  }
  
  
  //  All should be done, do last minute checking before we return a completed user
  if(newUser != null)
    users.add( newUser );  
  
  //add the new user as an egg cell
  createCellFromUser( newUser );    
  
  if(newUser!=null)
    return newUser;
  
  //failiure to load...    
  return null;  
}

User findUserByHandle(String username){
  username = username.toLowerCase();
  for(int i=0; i<users.size(); i++){
    User user = (User) users.get(i);
    if(username.equals( user.handle==null? user.handle : user.handle.toLowerCase()) || username.equals( user.contactLASTFM==null? user.contactLASTFM : user.contactLASTFM.toLowerCase()) || username.equals( user.contactFLICKR==null? user.contactFLICKR : user.contactFLICKR.toLowerCase()))    
      return user;
 /*    
    String handle;
    if(user.handle==null)
      handle = null;
    else
      handle = user.handle.toLowerCase();
      
    String contactLASTFM;
    if(user.contactLASTFM==null)
      contactLASTFM = null;
    else
      contactLASTFM = user.contactLASTFM.toLowerCase();
    
    String contactFLICKR;
    if(user.contactFLICKR==null)
      contactFLICKR = null;
    else
      contactFLICKR = user.contactFLICKR.toLowerCase();

    if(username.equals( handle ) || username.equals( contactLASTFM ) || username.equals( contactFLICKR ) )  
*/
  }
  return null;
}

void linkFriends(){
  for(int i=0; i<users.size(); i++){
    User user = (User) users.get(i);
    
    //link flickr friends
    if(user.flickrFriendsHandle!=null){
      if(user.flickrFriendsHandle.length!=0){
        for(int s=0; s<user.flickrFriendsHandle.length; s++){
          for(int b=0; b<users.size(); b++){
            User friend = (User) users.get(b);
            if(friend.contactFLICKR!=null)
              if(friend.contactFLICKR.equals(user.flickrFriendsHandle[s])){
                user.flickrFriends.add(friend);
                user.cell.friendPath.add(new Vec2());
                user.cell.friendPathTime.add(new Float( (float) random(1) ));
                
                friend.flickrFriends.add(user);
                friend.cell.friendPath.add(new Vec2());   
                friend.cell.friendPathTime.add(new Float( (float) random(1) ));                
              }
          }
        }
      }
    }
    
    //link lastfm friends
    if(user.lastfmFriendsHandle!=null){
      if(user.lastfmFriendsHandle.length!=0){
        for(int s=0; s<user.lastfmFriendsHandle.length; s++){
          for(int b=0; b<users.size(); b++){
            User friend = (User) users.get(b);
            if(friend.contactLASTFM!=null)
              if(friend.contactLASTFM.equals(user.lastfmFriendsHandle[s])){
                user.lastfmFriends.add(friend);
                user.cell.friendPath.add(new Vec2());
                user.cell.friendPathTime.add(new Float( (float) random(1) ));                
                
                friend.lastfmFriends.add(user);
                friend.cell.friendPath.add(new Vec2());                
                friend.cell.friendPathTime.add(new Float( (float) random(1) ));                
              }
          }
        }
      }
    }    
  }
}
