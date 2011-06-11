//User Displays
//This file contains functions that are used in displaying user-content
//This file also contains user-history log.


int maxUserLog = 6;
Vector userLog = new Vector();
//Vec2 userLogPos = new Vec2(10,100);
Vec2 userLogPos = new Vec2(35,50);
boolean displayUserLog = false;

void addToUserLog(Cell cell){
  if(cell==null)
    return;

  for(int i=0;i<userLog.size();i++){
    User user = (User) userLog.get(i);
    if(user!=null)
      if(cell.user == user)
        return;  
  }

  userLog.add(0, cell.user);
  if(userLog.size() > maxUserLog)
    userLog.remove(userLog.lastElement());
}


void drawUserLog(){
  //  if(!aCellSelected)
  //    return;
  if(!draggableHistory.using)
    return; 


  pushMatrix();
  translate(draggableHistory.pos);
  translate(0,-25);
  /*  
   if(userLog.size()>0){
   textFont(magnetFont,16);
   textAlign(LEFT);
   noStroke();
   fill(228,230,250,80);
   text("history",0,30);
   }*/

  textFont(magnetFont,12);
  textAlign(LEFT);
  noStroke();  

  float xpos = 0;
  float ypos = 0;
  float opa = 200;
  for(int i=0; i<userLog.size(); i++){    

    if(i%2==0)
      xpos = 20;
    else
      xpos = 0;
    translate(0,40);    
    User user = (User) userLog.get(i);
    user.cell.inHistoryLog = true;

    //    image(glow,xpos-glowSize,-glowSize);
    //    tint(255,opa);
    noSmooth();
    image(user.thumbnail,xpos,0);
    smooth();
    float vx = xpos  + camPos.x;
    float vy = 18 + camPos.y;

    float sx = screenX(vx,vy);
    float clipX = sx;
    if(user.cell.m.p.x > sx)
      sx += 40;    
    else
      sx -= 4;
    float sy = screenY(vx,vy);
    float clipY = sy;
    user.cell.historyPos.nowEquals( sx , sy );

    boolean mouseOverUser = false;
    if(mp.x > clipX && mp.x < clipX + 38)
      if(mp.y > clipY - 20 && mp.y < clipY + 20){
        mouseOverUser = true;
      }

    if(mouseOverUser){
      user.cell.useHistoryLine();
      mouseOverCell = true;
      hoveringCell = user.cell;      
    }
    //holy shit    
    if(mouseOverUser && mousePressed && !draggingDraggable && !draggingScreen && !draggingMagnet && !mouseOverMagnet && !mouseOverDraggable && !mouseOverQuery){
      if(selectedCell != null){
        if(selectedCell.user!=null)
          selectedCell.user.clearImages();
      }
      selectCell(user.cell);        
      setUserToDraw(selectedCell);      
      if(!sound.disableSound)      
        sound.itemClickSound.play();
    }


    fill(opa);    
    //text(user.handle + " listens to ", xpos + 42, 16);
    text(user.handle, xpos + 42, 16);    
    //text(user.listensTo, xpos + 160, 16);

    opa -= 15;
  }
  popMatrix();
}

String userFormatted = "";
int userDataCharNum = 0;
void setUserToDraw(Cell cell){
  userFormatted = cell.user.formatted;
  userDataCharNum = 0;
}

float lastfmTagX = 0;
float flickrTagX = 0;

static final float imageSpacing = 116;
static final float selectedArtBorder = 4;
static final float textSpacingFromArt = 14;

boolean mouseOverArt = false;
String artLink = "";
String lastArtLink = "";

Vec2 artSelectCornerA = new Vec2();
Vec2 artSelectCornerB = new Vec2();

Vec2 artSelectCornerADest = new Vec2();
Vec2 artSelectCornerBDest = new Vec2();

void resetUserDisplay(){
  lastfmTagX = -50;
  flickrTagX = -50;      
  artSelectCornerA.nowEquals(-150 , height + 100);
  artSelectCornerB.nowEquals(   width + 150 , height + 150);  
  artSelectCornerADest.nowEquals(-150 , height + 100);
  artSelectCornerBDest.nowEquals(   width + 150 , height + 150);  
}

Vec2 userProfilePos = new Vec2(-10,300);
void drawUserData(){   
  if(!aCellSelected){
    lastfmTagX = -50;
    flickrTagX = -50;    
    mouseOverArt = false;
    return;
  }      

  mouseOverArt = false;    

  User user = selectedCell.user;  
  if(user==null)
    return;  

  pushMatrix();
  translate(userProfilePos);  
  if(largeApp)
    translate(0,270);
  if(user.flickrUser)
    if(user.flickrArt!=null)
      if(user.flickrArt.length > 0)
        translate(0,-120);

  if(user.lastfmUser)
    if(user.lastfmArt!=null)
      if(user.lastfmArt.length > 0)
        translate(0,-120);        

  //  if(frameCount%==0)
  userDataCharNum+=3;
  if(userDataCharNum >= userFormatted.length())
    userDataCharNum = userFormatted.length()-1;  
  String sub = userFormatted.substring(0,userDataCharNum);
 
  noSmooth();
  if(user.thumbnail!=null)
    image(user.thumbnail,20,20);  
  smooth();  

  fill(255);
  textFont(guiFont,18);    
  textAlign(LEFT);
  text(sub, 62, 36);  

  if(user.flickrUser && user.flickrArt!=null){      

    pushMatrix();
    translate(62,200);

    translate(-30,0);    

    textAlign(CENTER);
    textFont(tinyguiFont,10);
    noSmooth();
    fill(255);

    //draw images for flickr
    for(int i=0;i<user.flickrArt.length;i++){
      if(user.flickrArt[i] != null){

        float spacingX = 0;
        if(user.flickrArt[i].width < 100)
          spacingX = 100 - user.flickrArt[i].width;

        pushMatrix();  

        //for flickr images that don't conform to 100 pixels
        translate(spacingX/2,0);

        //find actual dimensions of the image
        float x1 = screenX(0,-user.flickrArt[i].height/2);
        float x2 = screenX(user.flickrArt[i].width , user.flickrArt[i].height/2);
        float y1 = screenY(0,-user.flickrArt[i].height/2);
        float y2 = screenY(user.flickrArt[i].width , user.flickrArt[i].height/2);

        //check if our mouse is hovering over the image
        if(mouseX > x1 && mouseX < x2 && mouseY > y1 && mouseY < y2){

          artSelectCornerADest.nowEquals(x1, y1);
          artSelectCornerBDest.nowEquals(x2, y2);

          mouseOverArt = true;

          lastArtLink = artLink;                       
          artLink = user.flickrArtLink[i];
          if(!lastArtLink.equals(artLink)){
            //            sound.uiPop.cue(0);
            if(!sound.disableSound)
              sound.uiPop.play();
          }

          //now fill the text with full white once highlighted          
          fill(255);
        }
        else
          fill(240);           

        if(user.flickrArt[i]!=null)        
          image(user.flickrArt[i], 0 ,-user.flickrArt[i].height/2);
        text(user.flickrArtName[i], user.flickrArt[i].width/2 , user.flickrArt[i].height/2 + textSpacingFromArt);      
        popMatrix();
        translate(imageSpacing,0);
        //        translate(user.flickrArt[i].width+16,0);
      }
    }
    popMatrix();        

    flickrTagX = lerp(flickrTagX, 50, .2);

    if(user.flickrArt.length>0){
      pushMatrix();
      translate(flickrTagX,155);
      textAlign(LEFT);
      textFont(tinyguiFont,12);
      noSmooth();
      drawTextBacking(-40,-12,250,15);      
      fill(255);      
      text("recently posted a picture of...",0,0);  
      popMatrix();        
    }          
  }

  //draw images for lastfm    
  if(user.lastfmUser && user.lastfmArt!=null){     

    pushMatrix();            
    translate(62,200);    

    //move down if also doing flickr images
    if(user.flickrUser && user.flickrArt.length > 0)    
      translate(0,120);       

    translate(-30,0);     

    textAlign(CENTER);
    textFont(tinyguiFont,10);
    noSmooth();
    fill(255);    

    for(int i=0;i<user.lastfmArtName.length;i++){
      if(user.lastfmArt !=null){

        //find actual dimensions of the image
        float x1 = screenX(0,-user.lastfmArt[i].height * .5);
        float x2 = screenX(user.lastfmArt[i].width , user.lastfmArt[i].height * .5);
        float y1 = screenY(0,-user.lastfmArt[i].height * .5);
        float y2 = screenY(user.lastfmArt[i].width , user.lastfmArt[i].height * .5);

        //check if our mouse is hovering over the image
        if(mouseX > x1 && mouseX < x2 && mouseY > y1 && mouseY < y2){          

          artSelectCornerADest.nowEquals(x1, y1);
          artSelectCornerBDest.nowEquals(x2, y2);

          mouseOverArt = true;

          lastArtLink = artLink;                              
          artLink = user.lastfmArtLink[i];  
          if(!lastArtLink.equals(artLink)){
            //            sound.uiPop.cue(0);
            if(!sound.disableSound)
              sound.uiPop.play();
          }


          //now fill the text with full white once highlighted          
          fill(255);
        }
        else
          fill(240);           

        if(user.lastfmArt[i]!=null)
          image(user.lastfmArt[i], 0, -user.lastfmArt[i].height/2);
        text(user.lastfmArtName[i], user.lastfmArt[i].width/2 , user.lastfmArt[i].height/2 + textSpacingFromArt);      
        //        translate(user.lastfmArt[i].width+12,0);        
        translate(imageSpacing,0);        
      }
    }
    popMatrix();

    lastfmTagX = lerp(lastfmTagX, 50, .15);
    if(user.lastfmArt.length>0){      
      pushMatrix();
      if(user.flickrUser && user.flickrArt.length > 0)    
        translate(0,120);        
      translate(lastfmTagX,155);
      textAlign(LEFT);
      textFont(tinyguiFont,12);
      noSmooth();      
      drawTextBacking(-40,-12,210,15);   
      fill(255);
      text("was just listening to...",0,0);  
      popMatrix();    
    }    

  }  

  popMatrix();

  //make sure we have any form of artwork before we use the selection box  
  if( (user.lastfmUser && user.lastfmArt!=null && user.lastfmArt.length > 0) || (user.flickrUser && user.flickrArt!=null && user.flickrArt.length > 0) ){
    //draw a selection rectangle around hovered image  
    lerp(artSelectCornerA, artSelectCornerA, artSelectCornerADest, .20);
    lerp(artSelectCornerB, artSelectCornerB, artSelectCornerBDest, .20);

    smooth();
    rectMode(CORNERS);          
    stroke(245,90,138);
    noFill();  
    rect(artSelectCornerA.x - selectedArtBorder,        artSelectCornerA.y - selectedArtBorder, 
    artSelectCornerB.x + selectedArtBorder + 1 ,   artSelectCornerB.y + selectedArtBorder + 1);
  }

}


void drawTextBacking(float x, float y,float length, float height){
  pushMatrix();
  translate(x,y);
  fill(0,200);
  noStroke();
  beginShape();
  vertex(0,0);
  vertex(length, 0);
  vertex(length + 8, height);
  vertex(0, height);
  endShape(CLOSE);
  popMatrix();
}
