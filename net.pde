//honestly
//this is my first time doing netcode
//bear with me!

String GOusersString;
String GOserverURL           = "http://fidgt.com/ws/xml/user?email=";
String GOgetUsersURL         = GOserverURL + "users";
String LASTFMserverURL       = "http://ws.audioscrobbler.com/1.0/user/";
String LastFMartistURL       = "http://ws.audioscrobbler.com/1.0/artist/";
String FLICKRserverURL       = "http://www.flickr.com/services/rest/";
String FLICKRAPIString       = "88fd7156c792ae3b22c50c0e7dd63921";
String FLICKRgetNSID         = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.people.findByUsername" + "&username=";
String FLICKRgetInfo         = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.people.getInfo" + "&user_id=";
String FLICKRgetFriends      = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.contacts.getPublicList" + "&user_id=";
String FLICKRgetTags         = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.tags.getListUserPopular" + "&count=10000" + "&user_id=";
String FLICKRgetFavorites    = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.favorites.getPublicList" + "&per_page=6" + "&user_id=";
String FLICKRgetSearch       = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.photos.search" + "&sort=date-posted-desc" + "&per_page=6" + "&user_id=";
String FLICKRgetSizes        = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.photos.getSizes" + "&photo_id=";
String FLICKRgetPhotoInfo    = FLICKRserverURL + "?api_key=" + FLICKRAPIString + "&method=flickr.photos.getInfo" + "&photo_id=";
//http://www.flickr.com/services/rest/?api_key=88fd7156c792ae3b22c50c0e7dd63921&method=flickr.people.findByUsername&username=bahamut0


int initGOUsers = 0;
int usersToLoadAtOnce = 15; //18 is a good number
int gearonUsersToLoadAtOnce = 10;

int totalFlickrFriendsExpected = 0;
int totalFlickrFriendsAdded = 0;

int loadTimer = 0;
Vector networkStreams = new Vector();

//this thread will load a flickr user via user name
class StreamFlickrUser extends Thread
{
  int id;
  User user;
  String query;
  
  //the textfield that called this load  
  TextField field;
  StreamFlickrUser(TextField field, String query){
    this.field = field;
    this.id = networkStreams.size();
    this.query= query;//.toLowerCase();
    start();
  } 
  void run(){
    user = loadFLICKRuser(query);
    
    
    boolean userAlreadyLoaded = false;
    
    if(user == null){
      //before we give up on this user
      //check if this user is already loaded. if so, just load friends
      
      user = findUserByHandle(query);   
      
      //alright, seriously not found, go ahead and quit thread
      if(user == null)                 
        quitThisThread();
      userAlreadyLoaded = true;
    }
            
    //load this user's friends
    String friends[] = user.flickrFriendsHandle;
    for(int i=0; i<friends.length; i++){
      networkStreams.add( new StreamFlickrFriend( friends[i], user ) );
    }
      
    //tells the cell to spawn off textfield instead of some random spot in space  
    if(!userAlreadyLoaded)
      user.cell.startFromField( field );
    
    //tells the cell that friends are spawning off of it
    if(!userAlreadyLoaded)
      user.cell.friendSpawn();
    else
      selectUser(user);
      
    if(!userAlreadyLoaded){  
      if(user.contactFLICKR!=null)
        user.getFLICKRData();
      if(user.thumbnailURL!=null) 
        if(!user.processGearOnThumbnail())
          user.hasNoThumbnail();

      user.formatText();   
      user.cell.rebuild();        
    }
   
    quitThisThread();
  }
  
  void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  void trySleep(){
    try {
      sleep(10);
    } 
    catch (InterruptedException e) {
    }        
  }
}

//this thread will load a flickr user via user name
class StreamLastfmUser extends Thread
{
  int id;
  User user;
  String query;
  
  boolean loadFriends;
  
  User sourceUser;
  
  //the textfield that called this load  
  TextField field;
  StreamLastfmUser(TextField field, String query, boolean loadFriends, User sourceUser){
    this.field = field;
    this.id = networkStreams.size();
    this.query= query; //.toLowerCase();
    this.loadFriends = loadFriends;
    this.sourceUser = sourceUser;
    start();
  } 
  void run(){
    user = loadLASTFMuser(query);
    
    
    boolean userAlreadyLoaded = false;
    
    if(user == null){
      //before we give up on this user
      //check if this user is already loaded. if so, just load friends
      
      user = findUserByHandle(query);   
      
      //alright, seriously not found, go ahead and quit thread
      if(user == null)                 
        quitThisThread();
      userAlreadyLoaded = true;
    }
            
    //load this user's friends
    if(loadFriends){
      String friends[] = user.lastfmFriendsHandle;
      for(int i=0; i<friends.length; i++){
        networkStreams.add( new StreamLastfmUser(null, friends[i], false, user ) );
      }
    }
      
    //tells the cell to spawn off textfield instead of some random spot in space  
    if(loadFriends){
      if(!userAlreadyLoaded)
        user.cell.startFromField( field );
    }
    else{      
      //tells the cell to spawn off a cell instead of some random spot in space  
      if(sourceUser!=null)
        user.cell.startFromCell( sourceUser.cell );      
    }
    
    //tells the cell that friends are spawning off of it
    if(loadFriends){
      if(!userAlreadyLoaded)
        user.cell.friendSpawn();
      else
        selectUser(user);
    }
      
    if(!userAlreadyLoaded){  
      if(user.contactLASTFM!=null)       
        user.getLASTFMData();
      if(user.thumbnailURL!=null) 
        if(!user.processGearOnThumbnail())
          user.hasNoThumbnail();          

      user.formatText();   
      user.cell.rebuild();        
    }
   
    quitThisThread();
  }
  
  void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  void trySleep(){
    try {
      sleep(10);
    } 
    catch (InterruptedException e) {
    }        
  }
}

//this thread will load a flickr user via user name
class StreamFidgtUser extends Thread
{
  int id;
  User user;
  String query;
  
  boolean loadFriends;
  
  User sourceUser;
  
  //the textfield that called this load  
  TextField field;
  StreamFidgtUser(TextField field, String query, boolean loadFriends, User sourceUser){
    this.field = field;
    this.id = networkStreams.size();
    this.query= query; //.toLowerCase();
    this.loadFriends = loadFriends;
    this.sourceUser = sourceUser;
    start();
  } 
  void run(){
    user = loadGOuser(query);
    
    
    boolean userAlreadyLoaded = false;
    
    if(user == null){
      //before we give up on this user
      //check if this user is already loaded. if so, just load friends
      
      user = findUserByHandle(query);   
      
      //alright, seriously not found, go ahead and quit thread
      if(user == null)                 
        quitThisThread();
      userAlreadyLoaded = true;
    }
    
      
    //add friends for both types      
    //load this user's friends
    if(loadFriends){
      String friends[] = user.lastfmFriendsHandle;
      for(int i=0; i<friends.length; i++){
        networkStreams.add( new StreamLastfmUser(null, friends[i], false, user ) );
      }
      
      friends = user.flickrFriendsHandle;
      for(int i=0; i<friends.length; i++){
        networkStreams.add( new StreamFlickrFriend( friends[i], user ) );
      }      
    }
    
      
    //tells the cell to spawn off textfield instead of some random spot in space  
    if(loadFriends){
      if(!userAlreadyLoaded)
        user.cell.startFromField( field );
    }
    else{      
      //tells the cell to spawn off a cell instead of some random spot in space  
      if(sourceUser!=null)
        user.cell.startFromCell( sourceUser.cell );      
    }
    
    
    //tells the cell that friends are spawning off of it
    if(loadFriends){
      if(!userAlreadyLoaded)
        user.cell.friendSpawn();
      else
        selectUser(user);
    }
      
      
    if(!userAlreadyLoaded){  
      if(user.contactLASTFM!=null)       
        user.getLASTFMData();
          
      if(user.contactFLICKR!=null)
        user.getFLICKRData();
                
      if(user.thumbnailURL!=null) 
        if(!user.processGearOnThumbnail())
          user.hasNoThumbnail();

      user.formatText();   
      user.cell.rebuild();        
    }
   
    quitThisThread();
  }
  
  void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  void trySleep(){
    try {
      sleep(10);
    } 
    catch (InterruptedException e) {
    }        
  }
}

//this thread will load a flickr friend via user name
class StreamFlickrFriend extends Thread
{
  int id;
  User user;
  String query;
  
  User sourceUser;
  StreamFlickrFriend(String query, User sourceUser){
    this.sourceUser = sourceUser;
    this.id = networkStreams.size();
    this.query= query;
    start();
  } 
  void run(){
    user = loadFLICKRuser(query);
    
    if(user == null)  
      quitThisThread();     
      
    //tells the cell to spawn off a cell instead of some random spot in space  
    user.cell.startFromCell( sourceUser.cell );
      
    if(user.contactFLICKR!=null)
      user.getFLICKRData();
    if(user.thumbnailURL!=null) 
      if(!user.processGearOnThumbnail())
        user.hasNoThumbnail();

    user.formatText();   
    user.cell.rebuild();        
   
    quitThisThread();
  }
  
  void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();    
  }
  
  void trySleep(){
    try {
      sleep(10);
    } 
    catch (InterruptedException e) {
    }        
  }
}


class StreamImage extends Thread
{
  User user;
  boolean started = false;
  boolean finished = false;
  boolean halt = false;
  StreamImage(User user){
    this.user = user;
  }
  void run(){    
    started = true;
    while(true){
      while(finished){
        try{sleep(10);}catch(Exception e){
          println(e);
        }
      }      
      halt = false;      
      finished = loadImages();            
    }
    
  }
  
  void restart(){
    finished = false;
    //start();
  }
  
  boolean loadImages(){
    if(halt)
      return true;        
    if(user.lastfmUser)
      user.loadLastFMArt();
    try{sleep(10);} catch(Exception e){}      
    if(halt)
      return true;    
    if(user.flickrUser)
      user.loadFlickrArt();
    try{sleep(10);} catch(Exception e){}          
    if(halt)
      return true;          
      
    return true;
  }
  void clear(){
    finished = true;
//    halt = true;
  }
}

//Properly formats a URL (spaces, special characters)
String formatURL(String text){   
  return java.net.URLEncoder.encode(text);    
}


//Network Stream log
String   streamLog[]     = {"", "", "", "", "", "", "", "", "", "" ,"" ,""};          //holds all messages from network log
int      logSpacing      = 9;                                                         //how many pixels per line in network log
boolean  showStreamLog   = true;                                                      //whether or not to draw network log

//Add a line to the network log
void addToStreamLog(String s){
  for(int i=streamLog.length-1; i>=1; i--){
    streamLog[i] = streamLog[i-1];
  }
  streamLog[0] = s;
}

//Draw the network log
void drawStreamLog(){
  if(!draggableLog.using)
    return;
  pushMatrix();
  translate(draggableLog.pos);
  translate(-30,35);
  noSmooth();
  noStroke();
  fill(#99B291,90);
  textAlign(LEFT);
  textFont(tinyguiFont,10);  
  
  text("input log",0,0);
  for(int i=0; i<streamLog.length; i++){    
    translate(0,logSpacing);    
    
    text(streamLog[i],0,0);    
  }
  
  popMatrix();
  smooth();
}

void printLog(String s){
  addToStreamLog(s);
  //println(s);
}
