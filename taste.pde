String genreTypes[] = {"blues", "alternative", "eclectic", "electronic", "hip hop", "classical", "soundtracks", "dance", "pop", "jazz", "reggae", "emo", "indie", "rock", "ambient", "experimental", "drum and bass", "rap"};

Hashtable allTags=new Hashtable();

void addToGlobalTags(Tag tag){
  //first do a check to see if it already exists
  if(allTags.contains(tag))
    return;
  
  String key = tag.name;
  allTags.put(key,tag);
}

class Tag{
  String name;
  float weight;
  Tag(String name, float weight){
    this.weight = weight;
    this.name = name;
  }
}
//String randomNames[] = {"Jake", "Jack", "Jane", "Mike", "Michael", "Rob", "Ed", "Edward", "Robert", "Kate", "Sawyer", "Locke", "John", "Anderson", "Hiro", "Smith", "Mick", "Sol", "Charles", "Rick", "David", "Eric", "Peggy", "Jen", "Jenni", "Jennifer", "Kathy", "Jason"};

class Taste{
  int songs;
  int albums;
  int playcount;
  int photos;
  
  ArrayList tags = new ArrayList();
  
  String listensTo = "";
  String formatted = "";
  
  static final int maxTagsToShow = 6;
  
  Taste(){}
//  Below is stuff for randomly generated content  
//  MusicTaste(){
//    int numberOfTypes = (int) random(3,genreTypes.length);
//    list = new Genre[numberOfTypes];
//    for(int i=0; i<numberOfTypes; i++){
//      int randomGenre = (int) random(genreTypes.length);
//      float randomWeight = random(1);
//      list[i] = new Genre(genreTypes[randomGenre], randomWeight);      
//    }    
//  }
  /*
  void organizeUserData(){

    userImage = userAnon;
    
    for(int i=0; i<list.length; i++){
      listensTo += list[i].name;
      if(i!=list.length-1)
        listensTo += ", ";
      if(i%5==0 && i!=0 && i!=list.length-1)
        listensTo += "\n          ";
    }
    
    formatted += name + " (" + uid + ")" + "\n";
    formatted += songs + " songs and " + albums + " albums" +"\n";
    formatted += "loves " + listensTo + "\n";
    formatted += "lastfm: " + "\n";
    formatted += "flickr: " + "\n";       
    
  }*/
  
  void formatText(){
    int tagAmount = tags.size();
    int tagsLeft = tagAmount;
    int charsOfLine = 0;
    StringBuffer sb = new StringBuffer();
    String linebreak = "\n";
    String separator = ", ";
    for(int i=0; i<tagAmount; i++){      
      if(i > maxTagsToShow){
        sb.append(" and more (");
        sb.append(nfc(tagsLeft));
        sb.append(")   ");
        //formatted += " and more (" + tagsLeft + ")   ";
        break;      
      }
      Tag tag = (Tag) tags.get(i);      
      if(charsOfLine+tag.name.length() > 40){
        //println("going to next line!--------------------------");
        charsOfLine = 0;
        sb.append(linebreak);
        sb.append(tag.name);
        sb.append(separator);
        //formatted+="\n";
        //formatted+=tag.name += ", ";        
      }
      else{
        charsOfLine+=tag.name.length()+2;
        sb.append(tag.name);
        //formatted+= tag.name;                 
        if(i!=tagAmount-1)
          sb.append(separator);
//          formatted += ", ";      
      }
      tagsLeft--;
    }
    formatted = sb.toString();
    
  }
  
  void addTag(String name, float weight){
    Tag tag = new Tag(name, weight);
    tags.add(tag);  
    addToGlobalTags(tag);    
  }
  
  void addTags(ArrayList tags){
    for(int i=0; i<tags.size(); i++){
      Tag tag = (Tag) tags.get(i);
      this.tags.add(tag);
      addToGlobalTags(tag);      
    }
  }
  
  float weightFor(String tagName){
    for(int i=0; i<tags.size(); i++){
      Tag tag = (Tag) tags.get(i);
      if(tag.name.startsWith(tagName))
        return tag.weight;       
    }
    return -1;
  }  
  int numberOftags(){
    return tags.size();
  }
}
