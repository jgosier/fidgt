PApplet pApp = this;
Vector users = new Vector(500);

class User{
//  String name;
//  int uid;
  boolean gearonUser = false;
  boolean flickrUser = false;
  boolean lastfmUser = false;
  
  //handles
  String contactAIM;
  String contactMSN;
  String contactJABBER;
  String contactLASTFM;
  String contactFLICKR;
  String contactFLICKRNSID;  
  String contactFLICKRiconServer;
  String contactEVDB;
  String handle = "";
  String thumbnailURL;

  String tags[];
  
  PImage thumbnail;
  
  String formatted = "";
  
  String flickrArtURL[];
  PImage flickrArt[];
  String flickrArtName[];
  String flickrArtLink[];
  
  String lastfmArtURL[];
  PImage lastfmArt[];
  String lastfmArtName[];
  String lastfmArtArtist[];
  String lastfmArtLink[];  
  
  Vector flickrFriends = new Vector();
  String flickrFriendsHandle[];  
  
  Vector lastfmFriends = new Vector();
  String lastfmFriendsHandle[];
  
  Taste taste = new Taste();;
  
  Cell cell;
  
  StreamImage imageStreamer;
  
  User(){
    imageStreamer = new StreamImage(this);
  }
  
  boolean processGearOnThumbnail(){
    printLog("loading user image for " + handle);    
    PImage grabbedThumbnail;
    try{
      //println(thumbnailURL);
      grabbedThumbnail = loadImage(thumbnailURL);
      thumbnail = new PImage(35,35);
      thumbnail.copy(grabbedThumbnail,0,0,grabbedThumbnail.width,grabbedThumbnail.height,0,0,35,35);
      thumbnail.mask(userAlphaMask);
      return true;
    }catch (Exception e){
      printLog("couldn't load thumbnail for user " + handle);
      return false;
    }
  }
  void hasNoThumbnail(){
    thumbnail = userAnon;    
  }
  
  void getFLICKRData(){
    flickrUser = true;
    
    //this is temporary...
    //not getting flickr profile pic yet
    hasNoThumbnail();
    
    XMLInOut xmlLoader = new XMLInOut(pApp);
    XMLElement xmlParent = null;
    
    try{
      println(FLICKRgetInfo + contactFLICKRNSID);
      xmlParent = xmlLoader.loadElementFrom(FLICKRgetInfo + contactFLICKRNSID);
      printLog("loading flickr user profile: "+contactFLICKR);
    }catch(Exception e){
      printLog("reason: " + e);
      printLog("a user profile from FLICKR failed to load!");
      return;
    }
    xmlParent = xmlParent.getChild(0);
    XMLElement elements[] = xmlParent.getChildren();
    //xmlParent.printElementTree();
    for(int i=0; i<elements.length; i++){
      XMLElement e = elements[i];
      if(e.getName().equals("photos")){
        taste.photos=(int) valueOf(e.getChild(2).getChild(0).getText());
        break;
      }
    }
    printLog("flickr user " + contactFLICKR + " has " + taste.photos + " photos");
    
    if(handle.equals(""))
      handle = contactFLICKR;
    try{
      xmlParent = xmlLoader.loadElementFrom(FLICKRgetTags + contactFLICKRNSID);
      printLog("loading flickr user data: "+contactFLICKR);
    }catch(Exception e){
      printLog("FLICKR tags from " + contactFLICKR + " failed to load!");
      return;
    }
    
    xmlParent = xmlParent.getChild(0).getChild(0);
    XMLElement tagElements[] = xmlParent.getChildren();
    //xmlParent.printElementTree();
    printLog("flickr user " + contactFLICKR + " has " + tagElements.length + " tags");
    float maxWeight = 1;   
    
    ArrayList flickrTags = new ArrayList();
    
    for(int i=0; i<tagElements.length; i++){      
      String tagName = "";
      float tagWeight = 0;      
      try{
        tagName = tagElements[i].getChild(0).getText();
        tagWeight = tagElements[i].getFloatAttribute("count");
      }catch(Exception e){
         printLog(contactFLICKR + " has no tags");
         break;
//        printLog("reason " + e);        
//        printLog("element " + i + " failed to load for user " + handle);
      }
      
      if(tagWeight > maxWeight){
        maxWeight = tagWeight;
      }        
      flickrTags.add(new Tag(tagName, tagWeight));
    }               
    
    for(int i=0; i<flickrTags.size(); i++){
      Tag tag = (Tag) flickrTags.get(i);
      tag.weight /= maxWeight;
    }
    //sort the flickr tags now    
    shellSort(flickrTags, SORTTAGS);
    
    taste.addTags(flickrTags);
    
    //get a list of flickr thumbnails            
    XMLElement photoPage = xmlLoader.loadElementFrom(FLICKRgetSearch + contactFLICKRNSID);
    //println(FLICKRgetFavorites + contactFLICKRNSID);
    photoPage = photoPage.getChild(0);
    flickrArt        = new PImage[photoPage.countChildren()];   
    flickrArtURL     = new String[flickrArt.length];    
    flickrArtName    = new String[flickrArt.length]; 
    flickrArtLink    = new String[flickrArt.length];
    
    if(photoPage.hasChildren()){
      XMLElement photos[] = photoPage.getChildren();
      for(int i=0; i<photos.length;i++){
        int       farmID    = photos[i].getIntAttribute("farm");
        int       serverID  = photos[i].getIntAttribute("server");        
        int       imageID   = photos[i].getIntAttribute("id");                
        String    secretID  = photos[i].getAttribute("secret");                  
        flickrArtURL[i]     = flickrThumbnailURL( farmID, serverID, imageID, secretID);        
        flickrArtName[i]    = formatTitle(photos[i].getAttribute("title"), 10);        
        flickrArt[i]        = new PImage(100,80);
        
        //get the image link
        XMLElement photoData;
        try{
//          println(FLICKRgetPhotoInfo + imageID + "&secret=" + secretID);
          photoData = xmlLoader.loadElementFrom(FLICKRgetPhotoInfo + imageID + "&secret=" + secretID);
          
          //need to go one layer in first to get past rsp stat = "ok" ...
          XMLElement labels[] = photoData.firstChild().getChildren();          
          boolean foundURL = false;
          for(int v=0; v<labels.length; v++){
            if(labels[v].getName().equals("urls")){
              flickrArtLink[i]    = labels[v].firstChild().firstChild().getText();
//              println(flickrArtLink[i]);
              foundURL = true;
              break;
            }
          }
          if(!foundURL)
            flickrArtLink[i] = "";
        } catch(Exception e){
          println("loading art error: " + e);
        }
        
      }
    }
    
  }
  
  void loadFlickrArt(){
    for(int i=0; i<flickrArtURL.length; i++){
      try{
        flickrArt[i] = loadImage(flickrArtURL[i]);      
      } catch( Exception e){
        println("can't load image " + flickrArtURL[i]);
        println(e);  
        println(FLICKRgetFavorites + contactFLICKRNSID);    
      }
    }
  }
  
  String flickrThumbnailURL(int farmID, int serverID, int imageID, String secretID){
    String url = "http://farm"+farmID+".static.flickr.com/"+serverID+"/"+imageID+"_"+secretID+"_t"+".jpg";
    return url;
  }
  
  void getLASTFMData(){
    lastfmUser = true;
    
    //Must be local otherwise threads will crash this    
    XMLInOut xmlLoader = new XMLInOut(pApp);
    XMLElement xmlParent = null;
    
    try{
      xmlParent = xmlLoader.loadElementFrom(LASTFMserverURL + contactLASTFM + "/tags.xml");
      printLog("loading lastFM user profile: "+contactLASTFM);
    }catch(Exception e){
      //if the xml file could not be loaded it has to be created
      printLog("reason: " + e);
      printLog("a user from lastFM failed to load!");
      return;
    }    
    XMLElement tagElements[] = xmlParent.getChildren();
    
    float maxWeight = 1;
//    printLog(tagElements.length + " tags");      
    for(int i=0; i<tagElements.length; i++){      
      String tagName = "";
      float tagWeight = 0;      
      try{
        tagName = tagElements[i].getChild(0).getChild(0).getText();
        tagWeight = valueOf( tagElements[i].getChild(1).getChild(0).getText() );
      }catch(Exception e){
        printLog("reason " + e);        
        printLog("element " + i + " failed to load for user " + handle);
      }
      
      if(i==0){
        maxWeight = tagWeight;
        tagWeight = 1;
      }        
      taste.addTag(tagName, tagWeight / maxWeight);
    }    

    try{
      xmlParent = xmlLoader.loadElementFrom(LASTFMserverURL + contactLASTFM + "/profile.xml");
      //println("loading lastFM user profile: "+contactLASTFM);
    }catch(Exception e){
      //if the xml file could not be loaded it has to be created
      printLog("a user profile from lastFM failed to load!");
      println("error at url: " + LASTFMserverURL + contactLASTFM + "/profile.xml");
      return;
    }  
    XMLElement properties[] = xmlParent.getChildren();
    for(int i=0; i<properties.length; i++){
      if(properties[i].getName().equals("playcount"))
        taste.playcount = (int) valueOf(xmlParent.getChild(i).getChild(0).getText());
    }    
    
    //get album art and album names
    XMLElement recentTracks = xmlLoader.loadElementFrom(LASTFMserverURL + contactLASTFM + "/recenttracks.xml");
    XMLElement tracks[] = recentTracks.getChildren();       
    
    int numTracks = tracks.length;
    if(numTracks > 6)
      numTracks = 6;
      
    //printLog("user " + contactLASTFM + " has at least " + numTracks + " albums");
    
    //this stands to be contained in its own object
    lastfmArt         = new PImage[numTracks];
    lastfmArtURL      = new String[numTracks];
    lastfmArtArtist   = new String[numTracks];
    lastfmArtName     = new String[numTracks];
    lastfmArtLink     = new String[numTracks];
    imageMode(CORNERS);
    
    for(int i=0;i<numTracks;i++){
      XMLElement trackData[] = tracks[i].getChildren();
      
      for(int s=0;s<trackData.length;s++){
        String tagName = trackData[s].getName();
        
        if(tagName.equals("name")){          
          lastfmArtName[i] = trackData[s].getChild(0).getText(); 
        }
        
        if(tagName.equals("artist")){                    
          lastfmArtArtist[i] = trackData[s].getChild(0).getText();
                    
          //must get image now. but from what source?
          //only way to get artist image is through similarartists 
          //and you can't get album data since track data doesn't say which album it came from!
          
          XMLElement similarArtists;
          try{
            //all goes as planned
            similarArtists = xmlLoader.loadElementFrom(LastFMartistURL + formatURL(lastfmArtArtist[i]) + "/similar.xml");
            lastfmArtURL[i] = similarArtists.getAttribute("picture");    

            lastfmArt[i] = new PImage(100,80);

          }catch(Exception e){
            //oops... for some reason the text formatting didn't work out
            lastfmArtURL[i] = "";            
            lastfmArt[i] = new PImage(100,100);
          }
        }   

        if(tagName.equals("url")){
          lastfmArtLink[i] = trackData[s].getChild(0).getText();
        }
        
      }
    }    
    
    //append artist to song title
    for(int i=0; i<lastfmArtName.length; i++){
      lastfmArtName[i] = "(" + lastfmArtArtist[i] + ") " + lastfmArtName[i];          
      lastfmArtName[i] = formatTitle(lastfmArtName[i], 10);         
    }
  }  
  
  void loadLastFMArt(){
    for(int i=0; i<lastfmArtURL.length; i++){
      PImage imageBuffer;
      try{
        if(lastfmArtURL[i]!=null && !lastfmArtURL[i].equals("")){          
          imageBuffer = loadImage(lastfmArtURL[i]);        
          float imageWidth = imageBuffer.width;
          float imageHeight = imageBuffer.height;
                
          //sanity check...
          if(imageHeight==0)
            imageHeight = 1;
             
          float ratio = imageWidth / imageHeight;
    
          boolean landscape = false;
          if(imageWidth > imageHeight)
            landscape = true;            
              
          if(imageWidth > 100){
            imageWidth = 100;
            imageHeight = 100 / ratio;
//            if(landscape)
//              imageHeight = 100 / ratio;
//            else
//              imageHeight = 100 * ratio;
          }
          //lastfmArt[i] = imageBuffer;    
          lastfmArt[i] = new PImage((int) imageWidth, (int) imageHeight);
          float remainderWidth  = imageBuffer.width  < 100 ? 100 - imageBuffer.width  : 100;
          float remainderHeight = imageBuffer.height < 100 ? 100 - imageBuffer.height : 100;          
          lastfmArt[i].copy(imageBuffer,0,0,imageBuffer.width,imageBuffer.height, 0 , 0 ,int(imageWidth),int(imageHeight));                        
          try{Thread.sleep(5);} catch(Exception e){}
        }
      } catch( Exception e){
        println("can't load image " + lastfmArtURL[i]);
        println(e);
        println(LASTFMserverURL + contactLASTFM + "/recenttracks.xml");             
        println(LastFMartistURL + formatURL(lastfmArtArtist[i]) + "/similar.xml");
      }      
    }
  }
  
  String formatTitle(String text, int maxSpacing){
    //process the name if it's too long     
    String formatted = text;
    int letterCount = 0;
    for(int v=0; v<formatted.length(); v++){
      char c = formatted.charAt(v);
      letterCount++;
      //replace space with a \n
      if(letterCount > maxSpacing && c == ' '){
        formatted = formatted.substring(0,v) + "\n" + formatted.substring(v+1);            
        letterCount = 0;
      }
    }        
    return formatted;
  } 
  
  void formatText(){
    StringBuffer sb = new StringBuffer();
    String linebreak = "\n";
    //formatted = "";
    //formatted += handle + "\n";
    sb.append(handle);
    sb.append(linebreak);
    
    if(contactLASTFM!=null){
    //  formatted += "lastfm: " + contactLASTFM + "\n";
      sb.append("lastfm: ");
      sb.append(contactLASTFM);
      sb.append(linebreak);
    }
    if(contactFLICKR!=null){
    //  formatted += "flickr: " + contactFLICKR + "\n";
      sb.append("flickr: ");
      sb.append(contactFLICKR);
      sb.append(linebreak);
    }
      
    taste.formatText();
    //formatted += "tags: " + taste.formatted + "\n";
    sb.append("tags: ");
    sb.append(taste.formatted);
    sb.append(linebreak);
    formatted = sb.toString();
  }
  
  void streamImages(){
    if(imageStreamer.started == false)
      imageStreamer.start();
    else
      imageStreamer.restart();    
  }
  
  void clearImages(){
    //println("attempting to clear image cache for " + handle + "...");
    //imageStreamer.stop();
    imageStreamer.clear();
    
    if(lastfmArt != null)
      for(int i=0; i<lastfmArt.length; i++)
        lastfmArt[i] = new PImage(100,80);
    
    if(flickrArt != null)
      for(int i=0; i<flickrArt.length; i++)
        flickrArt[i] = new PImage(100,80);         
    
  }
  
  
  void setFlickrFriends(XMLElement friendsList){ 
    friendsList = friendsList.getChild(0);
    XMLElement friends[] = friendsList.getChildren();    
    
    flickrFriendsHandle = new String[friends.length];
    
    for(int s=0; s<friends.length;s++){
      flickrFriendsHandle[s] = friends[s].getAttribute("username");      
    }
  }
  
  void setLastfmFriends(XMLElement friendsList){
    XMLElement friends[] = friendsList.getChildren();    
    
    lastfmFriendsHandle = new String[friends.length];
    
    for(int s=0; s<friends.length;s++){
      lastfmFriendsHandle[s] = friends[s].getAttribute("username");      
    }    
  }
}


boolean userAlreadyExists(String username){
  username = username.toLowerCase();
  for(int i=0; i<users.size(); i++){
    User user = (User) users.get(i);       
    if(username.equals( user.handle==null? user.handle : user.handle.toLowerCase()) || username.equals( user.contactLASTFM==null? user.contactLASTFM : user.contactLASTFM.toLowerCase()) || username.equals( user.contactFLICKR==null? user.contactFLICKR : user.contactFLICKR.toLowerCase()))    
      return true;
  }
  return false;
}

float valueOf(String s){
  return Float.valueOf(s).floatValue();
}


void selectUser(User user){
  if(user==null)
    return;
  selectCell(user.cell);
}
