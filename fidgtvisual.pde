/**
  AMORPHIC is a lastfm/flickr/fidg't visualizer for promotion of the gearon network.
  The following code is written by Flux for Protohaus
**/

import proxml.*;
//proxml library is by christian reikoff
//used for loading lastfm/flickr/gearon api

import java.lang.*;
//we need this for threading

import processing.opengl.*;
//JOGL to make things draw uber fast

import javax.media.opengl.*; 
//direct opengl access

import pitaru.sonia_v2_9.*;
//trying amit's sound lib

//Construct
boolean   loadingFinished  = false;
boolean   largeApp         = true;
int       defWidth         = 0;
int       defHeight        = 0;

//Background
float     bgOpa            = 255;         //background opacity
float     bgOpaDest        = 255;         //what the bg opacity wants to be

//Images
PImage    bgImage;                        //background image
PImage    bgImageLarge;
PImage    goLogo;                         //gearon logo
PImage    glow;                           //holds the shadow image
float     glowSize;                       //preset size for glow, used for placement
PImage    tinyGlow;
PImage    userAnon;                       //blank user-image for users with no thumbnail
PImage    userAlphaMask;                  //hex mask for user images

//Graphics
GL        gl;
Seg       stippleEllipse[] = null;        //holds precalculated lines for stippled ellipse
color     helpColor        = #8CE357;     //default color for helptext

//Sound
//all sounds are kept in soundLoader thread
Sound     sound;

//Fonts
PFont     magnetFont;                     //font for magnets and query
PFont     guiFont;                        //font for user gui text
PFont     tinyguiFont;                    //console text font

//Mouse
Vec2      mp               = new Vec2();  //this holds the mouse position in a Vec2

//Camera
Vec2      camPos           = new Vec2();  //camera position
Vec2      camPosDest       = new Vec2();  //where camera wants to be, for smoothing
float     camZoom          = 0;           //used for camera zooming, not currently used
boolean   draggingScreen   = false;       //is the screen being dragged?
float     screenDragSpeed  = 17;          //how fast the screen is scrolled

void setup(){
  
  size(1024,768,OPENGL);
  
  //width and height
  defWidth = width;
  defHeight = height;    
    
  //amorphic is now frame-resizable
  frame.setResizable(true);    
  
  //framerate set to max of 50 so we don't move things way too fast
  frameRate(50);
  
  //hookup for direct opengl calls
  gl = ((PGraphicsOpenGL)g).gl;
  
  //load the background image
  if(!largeApp)
    bgImage             = loadImage("bg.jpg");  
  else
    bgImageLarge        = loadImage("bgLarge.jpg");
  
  //load logos
  goLogo                = loadImage("fidgtlogo.png");
  
  //userAnon image used when there is no thumbnail available
  userAnon              = loadImage("userAnon.jpg");
  userAlphaMask         = loadImage("userAlpha.jpg");
  userAnon.mask( userAlphaMask );    
  
  //load shadow image
  glow                  = loadImage("glowimage.jpg");         
  glow.mask( loadImage("iconbacklit.jpg") );
  glowSize              = glow.width/2;
  
  tinyGlow              = loadImage("tinyglow.jpg");
    
  //load our fonts
  magnetFont            = loadFont("HelveticaNeue-ExtBlackCondObl-24.vlw");
  guiFont               = loadFont("HelveticaNeue-HeavyItalic-18.vlw");
  tinyguiFont           = loadFont("HelveticaRounded-BlackObl-10.vlw");

  //start up Sonia
  Sonia.start(this);   //44100
  
  if(!largeApp)
    queryPos = new Vec2(360,240);  
  else
    queryPos = new Vec2(512,384);
        
  //initialize query's mass
  queryMass             = new mass(queryPos);

  //initialize the selection orbiters    
  initOrbiters();
  
  //initialize all draggables we want to use...
  initDraggables();
  draggableLog.using = true;
  
  //initialize text fields
  setupTextFields();  
  
  //pre-calculate points for query's stipple ellipse
  precomputeStippleEllipse(queryCustomRadius, queryStippleAmount);
  
  //write out help text  
  helpText += "Add yourself into the visualization using the search bars below\n";
  helpText += "Then click the query that says \"type here\" to create a magnet.\n";
  helpText += "Magnets can be any word or tag.\n";    
  helpText += "The white bubbles are users. They are attracted to magnets that \n";
  helpText += "contain their favorite music or photography!\n";  

  helpTextLines = split(helpText,'\n');
  
  if(largeApp)
    maxUserLog = 12;
    
  //use a thread to load sounds
  sound = new Sound();   
    
  //show credits
  printLog("  http://www.processing.org");                
  printLog("built with PROCESSING");            
  
}//end setup()

void draw(){
  
  //Do a loading sequence when we are loading in resources
  if(!loadingFinished)
    drawLoading();
  else{
    try{
      
      //This is our main draw loop
      drawMain();
    }
    
    catch(Exception e){
      println(e);
    }
  }
}

void drawLoading(){
  background(0);  
  manageBG();
  drawLogos();    
  textAlign(CENTER);
  textFont(magnetFont,24);
  fill(255);
  text("loading...", width/2,height/2);
}

void drawMain(){
  background(0);
  
  manageBG();     
  manageCam(); 
         
  //move to where the camera is pointing
  //everything drawn between push and pop will be on "relative" screen space existing with cells/magnets
  pushMatrix();  
  translate(-camPos.x, -camPos.y, 0);  
  drawLogos();
  drawCellHistoryLines();
  updateDraggables();
  drawDraggables();  
  drawUserLog();   
  drawStreamLog();  
  drawTextFields();    
  drawHelp();      
  
  updateQuery();    
  updateTextFields();
  manageMagnets();
  manageCursor();  
  updateMagnets();      
  updateCells();   
  updateOrbiters(selectedCell);  
  drawOrbiters(selectedCell);   
  
  popMatrix();

  draggingScreen();   
  
  drawUserData();            
}//end draw()

//This function handles what the mouse cursor looks like under certain conditions
void manageCursor(){  
//  if(!mouseOverCell  && !mouseOverArt)
//    cursor(ARROW);
//  else 
  if(draggingScreen || mouseOverMagnet || draggingMagnet || mouseOverDraggable || draggingDraggable)
    cursor(HAND);    
  else
  
  //if we're over a query, show a text cursor
  if((mouseOverQuery && queryUsable)|| mouseOverField)
    cursor(TEXT);
  else
  
  //eh, otherwise just show an arrow
    cursor(ARROW);  
}//end manageCursor()

//Draws the logo
void drawLogos(){  
  //custom shoutout
  textAlign(LEFT);
  textFont(magnetFont,12);
  fill(255,100);
  text("visualization mashup by Flux", width-155,height-16);  
  text("www.ghost-hack.com", width-122,height-04);    
  
  //gearon Logo
  image(goLogo, width - 155, height - 100);
}

//Manages the background fading in and out using tint()
void manageBG(){
  bgOpa = lerp(bgOpa, bgOpaDest, .2);
  if(aCellSelected || mouseOverMagnet)
    bgOpaDest = 50;
  else
    bgOpaDest = 240;
        
  //we gotta draw the bg image without smoothing to avoid a diagonal line
  //no point in doing so, anyway
  noSmooth();
  
  //tinting with just color, as opposed to opacity, is a lot faster
  tint(bgOpa,255);
  
  //Resize the background based on our app window
  pushMatrix();
  scale(width / 1024.0, height / 768.0);
  image(bgImageLarge,0,0);
  popMatrix();
    
  noTint();
  smooth();  
}//end of manageBG()


//manages camera
void manageCam(){  
  //okay... if we move the camera then the mouse position gets whacky
  //this gets the mouse pos relative to screen
  mp.nowEquals(mouseX + (camPos.x),mouseY + (camPos.y));
  
  //smoothly move camera to where it should be
  lerp(camPos, camPos, camPosDest, .2);
  
  //zoom is not used yet.
  //camZoom = lerp(camZoom, -200, .3);    
  
  //if a cell is selected, follow the cell with the cell at the center of screen
  if(aCellSelected && selectedCell!=null){
    Vec2 cellCamPos = new Vec2(selectedCell.m.p);
    cellCamPos.x   -= width/2;
    cellCamPos.y   -= height/2 - 100;
    camPosDest.nowEquals(cellCamPos);
  }  
}//end manageCam()


//allows mouse to drag the screen space around
void draggingScreen(){
  if(mousePressed && !mouseOverMagnet && !mouseOverDraggable && !mouseOverArt) {    
//    if(mouseOverCell)
//      return;
    //smoothly move camera to where mouse is going
    lerp(camPosDest, camPosDest, new Vec2(camPos.x + (pmouseX - mouseX) * screenDragSpeed, camPos.y + (pmouseY - mouseY) * screenDragSpeed), .1);    
    draggingScreen = true;
  }        
}//end draggingScreen()

//turns the value given to it into the absolute screen position
void absScreenVec(Vec2 v){
  v.nowEquals(v.x + camPos.x, v.y + camPos.y);
  return;
}//end absScreen()

//precalculates stipple ellipse so we don't do it every friggin frame
void precomputeStippleEllipse(float radius, int segments){
  stippleEllipse = new Seg[segments];
  
  Vec2 ori = new Vec2();
  float angle = 0;
  segments = segments*2;
  float angleDelta = 360.0 / (float) (segments);
  
  radius = radius/2; //damned radius messup from other parts of code  
  int s=0;
  
  //go in a circle, and save every other segment to the segment list
  for(int i=0; i<segments; i++){        
    if(i%2==0){
      Vec2 A        = newVec(angle, radius);
      Vec2 B        = newVec(angle+angleDelta, radius);
      Seg segment   = new Seg( A , B );
      stippleEllipse[s] = segment;
      s++;
    }          
    angle+=angleDelta;    
  }
  
}

//draw the stipple ellipse
void drawStippleEllipse(Vec2 pos){
  pushMatrix();
  translate(pos);
  for(int i=0; i<stippleEllipse.length; i++){
    line(stippleEllipse[i].a, stippleEllipse[i].b);
  }
  popMatrix();
}

class Sound extends Thread
{  
  
  boolean soundIsMute = false;
  boolean disableSound = false;
  
  Sample amorphicBGSound;
  Sample userPop;
  Sample userPopEgg;
  Sample uiPop;
  Sample itemClickSound;
  Sample itemClickOff;  
  Sample magnetHover;
  Sample magnetClick;
  Sample magnetClickOff;  
  Sample magnetHoverOff;  
  Sample historySound;
  Sample magnetCreate;
  
  Sound(){
    start();
  }
  void run(){
    if(disableSound){
      loadingFinished = true;      
      stop();
      return;
    }
    
    try{
      //load our sounds      
      
      //userPop                  = new Sample("userPop.wav");
           
      userPopEgg               = new Sample("userPopEgg.wav");      
      uiPop                    = new Sample("uipop.wav");
      itemClickSound           = new Sample("itemClick.wav");
      itemClickOff             = new Sample("itemClickOff.wav");
      magnetHover              = new Sample("magnet_tone.wav");    
      magnetHoverOff           = new Sample("magnet_tone_off.wav");       
      magnetClick              = new Sample("magnet_click.wav");
      magnetClickOff           = new Sample("magnet_click_off.wav");        
      historySound             = new Sample("history.wav");
      magnetCreate             = new Sample("magnetcreate.wav");     
      
      //amorphicBGSound          = new Sample("amorphic_bg.wav");        
    
      //amorphicBGSound.setVolume(.05);
      //amorphicBGSound.repeat();    
      loadingFinished = true;      
    }catch(Exception e){
      
      //NOTE**************
      //
      //  Intel macs have trouble with jsyn for some reason
      //
      //****************//
      
      //printLog("error: "+ e);
      printLog("  are you on an Intel-Mac? Sorry... sound doesn't currently work on your platform.");
      printLog("Sound load failed...");
      //println(e);
      disableSound = true;      
      loadingFinished = true;
    }
    
    try{
      sleep(10);
    }catch(Exception e){}
    //stop();
  }
  
  void muteSound(){
    if(disableSound)
      return;
    userPop.setVolume(0);
    userPopEgg.setVolume(0);    
    uiPop.setVolume(0);
    itemClickSound.setVolume(0);    
    itemClickOff.setVolume(0);
    magnetHover.setVolume(0);  
    magnetHoverOff.setVolume(0);
    magnetClick.setVolume(0);    
    magnetClickOff.setVolume(0);
    historySound.setVolume(0);        
    magnetCreate.setVolume(0);    
  }
  
  void unmuteSound(){
    if(disableSound)
      return;
    userPop.setVolume(1);
    userPopEgg.setVolume(1);    
    uiPop.setVolume(1);
    itemClickSound.setVolume(1);    
    itemClickOff.setVolume(1);
    magnetHover.setVolume(1);  
    magnetHoverOff.setVolume(1);
    magnetClick.setVolume(1);    
    magnetClickOff.setVolume(1);
    historySound.setVolume(1);        
    magnetCreate.setVolume(1);        
  }
  
  void toggleMute(){
    soundIsMute = !soundIsMute;
    
    if(soundIsMute)
      muteSound();
    else
      unmuteSound();
  }
}

//this stuff not yet working
void changeToFullScreen(){
//  frame.setVisible(false);
//  frame.setUndecorated(true);
  frame.setSize(1400, 1024);        
  frame.setLocation(0,0);   
//  frame.setVisible(true);
}

void changeToWindowed(){
//  frame.setUndecorated(false);
  frame.setSize(defWidth, defHeight);        
  frame.setLocation(0,0);           
}

public void resize(int iwidth, int iheight){  
  defWidth = iwidth;
  defHeight = iheight;
  super.resize(iwidth, iheight);
}

//clean up your mess
public void stop() {    
  //userStream.stop();
  //Ess.stop();  
  Sonia.stop();
  sound.stop();  
  super.stop();
}
