static final int USERSEARCH = 1;
static final int FINDFLICKRUSER = 2;
static final int FINDLASTFMUSER = 3;
static final int FINDFIDGT = 4;

boolean executeAuto(TextField field, int type, String query){
  boolean success = false;
  switch(type){
    case USERSEARCH:
      executeSearch(query);
      return true;
    case FINDFLICKRUSER:
      return findFlickrUser(field, query);
    case FINDLASTFMUSER:
      return findLastfmUser(field, query);
    case FINDFIDGT:
      return findFidgtUser(field, query);      
  }
  return success;
}


void executeSearch(String query){
  query = query.toLowerCase();
  boolean foundUser = true;
  for(int i=0; i<users.size(); i++){
    User user = (User) users.get(i);
    String handle = user.handle.toLowerCase();
    if(query.equals(handle)){
      foundUser = true;
      selectUser(user);
      if(!sound.disableSound)      
        sound.magnetCreate.play();            
      break;
    }
  }

  if(!foundUser){
    if(!sound.disableSound)    
      sound.magnetClickOff.play();       
  }  
}

String autoComplete(int type, String query){
  switch(type){
    case USERSEARCH:
      return autoCompleteSearch(query);
  }
  
  //no autocomplete
  return "";
}

String autoCompleteSearch(String query){
  query = query.toLowerCase();  

  // Iterate over all keys in the table 
  String auto = "";
  String autoPicked = "";
  Iterator it = users.iterator();
  int stringLength = 10000;
  while (it.hasNext()) {
    // Retrieve user
    User user = (User) it.next();
    auto = user.handle.toLowerCase();
    if(auto.startsWith(query)){
      //find the shortest string in comparison
      if(auto.length()<stringLength){
        stringLength = auto.length();
        autoPicked = user.handle;
      }
    }
  }
  //in case it finds nothing... it returns ""
  return autoPicked;  
}
