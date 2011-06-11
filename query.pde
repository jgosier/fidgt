String    initialQuery       = "type here to create a new magnet";
String    queryString        = initialQuery;
String    queryAuto          = "";
boolean   mouseOverQuery     = false;       
boolean   queryUsable        = true;
boolean   queryActive        = false;
boolean   queryTouched       = false;
boolean   useAutoString      = false;
Vec2      queryPos;
mass      queryMass;
float     queryCustomRadius  = 100;
float     queryRadius        = 100;
int       queryStippleAmount = 24;

//Autocompletes a query by searching through current list of tags
String autoComplete(String query){
  // Iterate over all keys in the table 
  String auto = "";
  String autoPicked = "";
  try{
    Iterator it = allTags.keySet().iterator();
    int stringLength = 10000;
    while (it.hasNext()) {
        // Retrieve key
        auto = (String)it.next();
        if(auto.startsWith(query)){
          //find the shortest string in comparison
          if(auto.length()<stringLength){
            stringLength = auto.length();
            autoPicked = auto;
          }
        }
    }    
  }
  //If current list of tags is being modified, simply return the query
  catch(ConcurrentModificationException e){
    return query;
  }
    
  //in case it finds nothing... it returns "" (an empty string)
  return autoPicked;
}

void updateQuery(){
  float queryDist = dist(mp,queryPos);
  mouseOverQuery = false;     
  if(queryDist<queryCustomRadius/2)
    mouseOverQuery = true;
  
  noFill();

  if(!mouseOverQuery && mousePressed)
    queryActive = false;
    
  if(queryActive || !queryTouched){
//  clicking on query lets you resize query
//  I want to remove this feature for now!
//    queryCustomRadius = queryDist * 2;
//    queryCustomRadius = constrain(queryCustomRadius,40,200);
  }
  
  strokeWeight(4);     
  if(queryActive){
    //color query darkest if we're using it
    stroke(0,255);    
    ellipse(queryPos,queryCustomRadius);
  }    
  else  
  if(mouseOverQuery){
    //color query thicker if mouse-over
    stroke(0,160);  
    drawStippleEllipse(queryPos);       
  }  
  else{
    //otherwise, style it dimmer
    stroke(0,80);      
    drawStippleEllipse(queryPos);
  }
  textFont(magnetFont,16);
  if((queryActive || mouseOverQuery || !queryTouched) && !useAutoString)
    fill(228,230,250);
  else
    fill(228,230,250,80);
  noStroke();
  textAlign(CENTER);
  textFont(magnetFont,16);    
    
  if(queryActive){
    String autoRemaining="";
    if(queryString.length()>0 && !queryAuto.equals(""))
      autoRemaining= queryAuto.substring(queryString.length());
    //String queryDisplay = queryString + autoRemaining;          

    text(queryString, queryPos.x, queryPos.y + 10);            
    if(frameCount%10==0)
      text("_", queryPos.x + textWidth(queryString) * .5, queryPos.y + 10);            
    
    //drawing autocomplete
    if(useAutoString)
      fill(228,230,250);
    else
      fill(198,200,210);    
    if(queryAuto.length()>1)
      text("( "+queryAuto+"? )", queryPos.x, queryPos.y + 25);
  }
  else
    text(queryString, queryPos.x, queryPos.y + 10);      
}
