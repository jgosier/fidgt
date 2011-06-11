import processing.core.*; import proxml.*; import java.lang.*; import processing.opengl.*; import javax.media.opengl.*; import pitaru.sonia_v2_9.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class fidgtvisual extends PApplet {/**
  AMORPHIC is a lastfm/flickr/fidg't visualizer for promotion of the gearon network.
  The following code is written by Flux for Protohaus
**/


//proxml library is by christian reikoff
//used for loading lastfm/flickr/gearon api


//we need this for threading


//JOGL to make things draw uber fast

 
//direct opengl access


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
int     helpColor        = 0xff8CE357;     //default color for helptext

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

public void setup(){
  
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

public void draw(){
  
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

public void drawLoading(){
  background(0);  
  manageBG();
  drawLogos();    
  textAlign(CENTER);
  textFont(magnetFont,24);
  fill(255);
  text("loading...", width/2,height/2);
}

public void drawMain(){
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
public void manageCursor(){  
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
public void drawLogos(){  
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
public void manageBG(){
  bgOpa = lerp(bgOpa, bgOpaDest, .2f);
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
  scale(width / 1024.0f, height / 768.0f);
  image(bgImageLarge,0,0);
  popMatrix();
    
  noTint();
  smooth();  
}//end of manageBG()


//manages camera
public void manageCam(){  
  //okay... if we move the camera then the mouse position gets whacky
  //this gets the mouse pos relative to screen
  mp.nowEquals(mouseX + (camPos.x),mouseY + (camPos.y));
  
  //smoothly move camera to where it should be
  lerp(camPos, camPos, camPosDest, .2f);
  
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
public void draggingScreen(){
  if(mousePressed && !mouseOverMagnet && !mouseOverDraggable && !mouseOverArt) {    
//    if(mouseOverCell)
//      return;
    //smoothly move camera to where mouse is going
    lerp(camPosDest, camPosDest, new Vec2(camPos.x + (pmouseX - mouseX) * screenDragSpeed, camPos.y + (pmouseY - mouseY) * screenDragSpeed), .1f);    
    draggingScreen = true;
  }        
}//end draggingScreen()

//turns the value given to it into the absolute screen position
public void absScreenVec(Vec2 v){
  v.nowEquals(v.x + camPos.x, v.y + camPos.y);
  return;
}//end absScreen()

//precalculates stipple ellipse so we don't do it every friggin frame
public void precomputeStippleEllipse(float radius, int segments){
  stippleEllipse = new Seg[segments];
  
  Vec2 ori = new Vec2();
  float angle = 0;
  segments = segments*2;
  float angleDelta = 360.0f / (float) (segments);
  
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
public void drawStippleEllipse(Vec2 pos){
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
  public void run(){
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
  
  public void muteSound(){
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
  
  public void unmuteSound(){
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
  
  public void toggleMute(){
    soundIsMute = !soundIsMute;
    
    if(soundIsMute)
      muteSound();
    else
      unmuteSound();
  }
}

//this stuff not yet working
public void changeToFullScreen(){
//  frame.setVisible(false);
//  frame.setUndecorated(true);
  frame.setSize(1400, 1024);        
  frame.setLocation(0,0);   
//  frame.setVisible(true);
}

public void changeToWindowed(){
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

//This uses Steve Rabin's "Recursive Dimensional Clustering"
//Read more about it in "Game Programming Gems II"
//Basic setup that this uses can be found on polygonal labs
//http://lab.polygonal.de/articles/recursive-dimensional-clustering/

//Rewritten here by Flux for Processing

static final int subdivThreshold = 4;
static final float RDCThreshold = .001f;

//Brute force collision detection
//Used on entire list or simply on a small cluster
public void bruteForce(ArrayList group){
  for(int i=0; i<group.size(); i++){
    for(int s=i+1; s<group.size(); s++){
      Cell cellA = (Cell) group.get(i);
      Cell cellB = (Cell) group.get(s);

      float distance = dist(cellA.m.p, cellB.m.p);
      float radiusTotal = cellA.radius + cellB.radius;
      if( distance < radiusTotal){
        if(!cellA.ignoreCollisions && !cellB.ignoreCollisions){
          sfDoublePD(cellA.m, cellB.m, distance, radiusTotal, .6f);
        }
      }           
    }
  }  
}

//Segments and sorts large lists into clusters for collision routine
public void recursiveClustering(ArrayList group, int axis1, int axis2){
  
  //if we have small enough of a cluster, go ahead and do collision detection
  if(axis1 == -1 || group.size() < subdivThreshold){
    bruteForce(group);
  }
  
  //okay we got bigger groups, split them up
  else{

    //get a list of boundaries for this group
    ArrayList boundaries = getOpenCloseBounds(group, axis1);
    
    //sort via our friendly speedycat sort routine
    shellSort(boundaries, SORTPOSITION);

    int newAxis1 = axis2;
    int newAxis2 = -1;
    boolean groupSubdivided =false;
    ArrayList subgroup = new ArrayList();
    int count = 0;

    for(int i=0; i<boundaries.size(); i++){
      Entity entity = (Entity) boundaries.get(i);
      if(entity.type == 0 ){
        count++;
        subgroup.add(entity.object);
      }
      else{
        count--;
        if(count == 0){
          if(i != (boundaries.size() - 1)){
            groupSubdivided = true;
          }
          if(groupSubdivided){
            if(axis1 == 0){
              newAxis1 = 1;
            }
            else
              if(axis1 == 1){
                newAxis1 = 0;
              }
          }
          
          recursiveClustering(subgroup, newAxis1, newAxis2);
          subgroup.clear();

        }
      }
    }
  }
}

//Returns an array of Entities, which basically hold one-dimensional bounds data
public ArrayList getOpenCloseBounds(ArrayList group, int axis){
  ArrayList boundaries = new ArrayList();
  int k = group.size();
  switch(axis){
  case 0:
    for(int i=0; i<k; i++){
      Cell cell = (Cell) group.get(i);
      boundaries.add( new Entity(0,  cell.m.p.x - cell.radius + RDCThreshold, cell));
      boundaries.add( new Entity(1, cell.m.p.x + cell.radius + RDCThreshold, cell));      
    }
    break;
  case 1:
    for(int i=0; i<k; i++){
      Cell cell = (Cell) group.get(i);
      boundaries.add( new Entity(0,  cell.m.p.y - cell.radius + RDCThreshold, cell));
      boundaries.add( new Entity(1, cell.m.p.y + cell.radius + RDCThreshold, cell));      
    }    
    break;
  default:
    break;
  }
  return boundaries;
}

//This holds one dimensional bounds data
//Tells where a bound starts, ends, where it is, and what object it's related to
class Entity{
  Cell object;
  int type;
  float  position;
  Entity(int type, float position, Cell object){
    this.object = object;
    this.type = type;
    this.position = position;
  }
}

static final int SORTPOSITION = 0;
static final int SORTTAGS = 1;

//shell sort helper
//in this case, we only have one type of thing we are sorting
public float sortBy(Object object, int type){
  switch(type){
  case SORTPOSITION:
    Entity entity = (Entity) object;
    return entity.position;
  case SORTTAGS:
    Tag tag = (Tag) object;
    try{Thread.sleep(1);} catch(Exception e){}
    return tag.weight;
  }
  return 0;  
}

//shell sort! super fast! yes!
//this algo is an epic win
//shell sort does not only sort shells
boolean stopRequested = false;
public void shellSort(ArrayList a, int sortType) { 
  int h = 1; /* * find the largest h value possible */
  while ((h * 3 + 1) < a.size()) { 
    h = 3 * h + 1; 
  } /* * while h remains larger than 0 */
  while( h > 0 ) { /* * for each set of elements (there are h sets) */
    for (int i = h - 1; i < a.size(); i++) { /* * pick the last element in the set */

      Object B = a.get(i); 
      int j = i; /* * compare the element at B to the one before it in the set * if they are out of order continue this loop, moving * elements "back" to make room for B to be inserted. */
      for( j = i; (j >= h) && (sortBy( a.get(j-h),sortType) > sortBy(B,sortType)); j -= h) {         
        if (stopRequested) { 
          return; 
        }
        a.set(j, a.get(j-h));
      } /* * insert B into the correct place */
      a.set(j, B);
    } /* * all sets h-sorted, now decrease set size */
    h = h / 3; 
  } 
}

static final int USERSEARCH = 1;
static final int FINDFLICKRUSER = 2;
static final int FINDLASTFMUSER = 3;
static final int FINDFIDGT = 4;

public boolean executeAuto(TextField field, int type, String query){
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


public void executeSearch(String query){
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

public String autoComplete(int type, String query){
  switch(type){
    case USERSEARCH:
      return autoCompleteSearch(query);
  }
  
  //no autocomplete
  return "";
}

public String autoCompleteSearch(String query){
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

ArrayList     cells             = new ArrayList(500);
int        numberOfCells     = 300;
Cell       selectedCell;
Cell       hoveringCell;
boolean    mouseOverCell     = false;
boolean    aCellSelected     = false;
float      springConstant    = .15f;
float      maxCellRadius     = 30;
float      eggSize           = 6;
 
public void createCellsFromUsers(){
  for(int i=0;i<users.size();i++){
    User user = (User) users.get(i);
    float cellRadius = 3;
    if(random(100)>99)
      cellRadius = random(12,40);
    else
    if(random(100)>90)
      cellRadius = random(5,12);
    else
      cellRadius = random(1,5);
    int randomDegree = PApplet.parseInt(random(0,3));    
    Cell cell = new Cell( user, new Vec2(random(width), random(height) ), cellRadius, 6 );    
    user.cell = cell;    
    cells.add(cell);
  }    
} 

public Cell createCellFromUser(User user){
  if(user == null)
    return null;
  
  float cellRadius = eggSize;
  /*
  if(random(100)>99)
    cellRadius = random(12,40);
  else
  if(random(100)>90)
    cellRadius = random(5,12);
  else
    cellRadius = random(1,5);
  int randomDegree = int(random(0,3));    */
  
  if(!sound.disableSound)
    sound.userPopEgg.play();
  Cell cell = new Cell( user, new Vec2(random(width), random(height) ), cellRadius, 6 );    
  user.cell = cell;  
  cells.add(cell); 
  return cell;
}
  
public void generateRandomUsers(){
  for(int i=0;i<numberOfCells;i++){
    float cellRadius = 3;
    if(random(100)>99)
      cellRadius = random(12,40);
    else
    if(random(100)>90)
      cellRadius = random(5,12);
    else
      cellRadius = random(1,5);
    int randomDegree = PApplet.parseInt(random(0,3));    
    Cell cell = new Cell( new User(), new Vec2(random(width), random(height) ), cellRadius, 6 );    
    cells.add(cell);
  }  
}  
  
class Cell{
  //Vec2 pos;
  mass m;
  float weight = .1f;
  SubCell[] subs;
  float radius;
  boolean hoveringOver = false;
  boolean lastHoverOver = false;
  boolean isSelected = false;
  boolean revealMagnet = false;
  boolean inHistoryLog = false;
  
  boolean smallCell = false;  
  
  float spinnerAngle = 0;
  
  //egg status is a cell that hasn't recieved user data
  boolean egg = true;
  
  User user;
  
  Vec2 historyPos = new Vec2();
  Vec2 historyAnchor = new Vec2();
  
//  MusicTaste taste;
  
  int cellColorSet = 0xffFFFFFF;
  int cellColor = 0xff000000;
  int cellMagnetColor = 0xff000000;
  static final int eggColor = 0xff849508;
  static final int eggOuterColor = 0xff8EFA47;
  
  float cellHistoryLineOpa = 255;  
  
  boolean leaveTrail = false;     
  
  boolean wandering = true;
  float wanderDistance;
  float wanderSpeed;
  float orientation;
  boolean ignoreCollisions = false;
  
  float sinWave = 0;
  
  boolean isFriendSpawn = false;
  
  Cell(User user, Vec2 pos, float radius, int sides){    
    this.user = user;
    
    weight = ( weight * radius) / 50;
    this.radius = radius;
    
    m = new mass(pos);
    if(sides<6){
      println("debug: attempted to make cell with less than six sides");      
      sides = 6;
    }           
    
    if(radius<=7){
      sides = 0;
      this.radius *=2;
      smallCell = true;
    }
    subs = new SubCell[sides];           
 
    if(!smallCell)   
      giveCellSides(sides);    
      
    orientation = random(-360,360);
    wanderDistance = random(queryRadius * 2, 450);
    wanderSpeed = random(.00001f, .0004f);
    
    sinWave = random(-100,100);
  }
  
  public void giveCellSides(int sides){
    if(sides!=0){
      float angle=0;
      float angleDelta = 360/(sides);
      
      if(sides<6)
        sides=6;
            
      for(int i=0; i<sides; i++){      
         Vec2 subPos = newVec(this.m.p, angle, radius);
         subs[i] = new SubCell(subPos);
         angle+=angleDelta;       
      }
    
      for(int i=0; i<sides; i++){
        m.connectSpringTo(subs[i].m, springConstant*3);      
        for(int u=0; u<sides; u++){
          if(i!=u)        
            subs[i].connectWith(subs[u]);
        }
      }      
    }    
  }
  
  public void startFromField( TextField field ){
    m = new mass(field.fieldX, field.fieldY);   
    Vec2 velocity = new Vec2(m.p, queryPos);
    
    //how much to shoot to the center
    float scaleAspectX = random(-.1f,.4f);
    float scaleAspectY = random(-.1f,.4f);    
    
    velocity.mul(scaleAspectX, scaleAspectY);
    m.v.nowEquals(velocity); 
  }
  
  public void startFromCell( Cell cell ){
    m = new mass(cell.m.p);      
    
    //how much to shoot to the center
    float scaleAspectX = random(-.4f,.4f);
    float scaleAspectY = random(-.4f,.4f);    
    Vec2 velocity = new Vec2(scaleAspectX, scaleAspectY);
    velocity.mul(scaleAspectX, scaleAspectY);
    m.v.nowEquals(velocity); 
  }  
  
  public void friendSpawn(){
    isFriendSpawn = true;
  }
    
  public void rebuild(){    
    //some equation to build radius
    radius =  14;
    radius += constrain((user.taste.numberOftags() / 6),0,40);
    radius += (user.taste.photos / 200);
    radius += (user.taste.playcount * user.taste.playcount) / 200000000;
    radius /= 2;
    
    weight = ( weight * radius) / 25;
    egg = false;    
    int sides = 6;
    if(radius<=7){
      sides = 0;
      smallCell = true;
    }
    else{
      if(radius > maxCellRadius)
        radius = maxCellRadius;
      smallCell = false;
      subs = new SubCell[sides];              
      giveCellSides(sides);      
    }
    if(!sound.disableSound)    
      sound.userPop.play();

  }
  
  public void update(){
    
    if(egg)
      sinWave+=.1f;
        
    try{
      
    if(selectedCell == this)
      isSelected = true;
    else
      isSelected = false;
      
    if(dist(mp,this.m.p)<radius){
      hoveringOver = true;    
      wandering = false;
      ignoreCollisions = true;
    }
    else{
      hoveringOver = false;
      ignoreCollisions = false;
    }
    
    revealMagnet = false;
    if((mouseOverMagnet || draggingMagnet) && hoveringMagnet!=null){
      CellAttraction ca = (CellAttraction) hoveringMagnet.cellsToAttract.get(this);
      if(ca!=null){
        revealMagnet = true;
        cellMagnetColor = ca.attractionColor;
      }
    }
     
    leaveTrail = false;
    if(egg){
      if(isFriendSpawn)
        cellColorSet = color(eggOuterColor);
      else
        cellColorSet = color(150,150,150);
    }
    else
    if(isSelected)
      cellColorSet = color(245,90,138);
    else
    if(hoveringOver)
      cellColorSet = color(247,192,215); 
    else      
    if(revealMagnet)
      cellColorSet = cellMagnetColor;
    else    
    if(mouseOverMagnet && !revealMagnet)
      cellColorSet = color(60);
    else
    if(aCellSelected)
      cellColorSet = color(60,120);    
    else{
      cellColorSet = color(255,245);
      leaveTrail = true;
    }
      
    if(leaveTrail)      
      cellColor = lerpColor(cellColor, cellColorSet, .02f);      
    else
      cellColor = lerpColor(cellColor, cellColorSet, .1f);      
      
    if(hoveringOver)
      cellHistoryLineOpa = 160;           
    
    if(lastHoverOver != hoveringOver && !egg){
      if(hoveringOver==true){
        if(!sound.disableSound)        
          sound.historySound.play();      
      }
    }
    
    if(isSelected)
      friendLinkOpa = lerp(friendLinkOpa, 120, .2f);
    else
    if(hoveringOver)
      friendLinkOpa = lerp(friendLinkOpa, 50, .2f);
    else
      friendLinkOpa = lerp(friendLinkOpa, 0, .2f);    
    
    lastHoverOver = hoveringOver;
        
    cellHistoryLineOpa = lerp(cellHistoryLineOpa, 0, .01f);
    

    if(wandering){
      sfSingle(m, queryMass, queryRadius, .01f);      
      sfSingle(m, queryMass, wanderDistance, .02f);
      //float ang = getAng(m.p, queryMass.p);
      //Vec2 forceForward = newVec(orientation + ang, wanderDistance);
      //forceForward.mul(wanderSpeed);
      //m.v.add(forceForward);
    } 
    
    if(ignoreCollisions){
      m.v.nowEquals(0,0);
    }
    
    m.update();
    m.dampScalar(FRICTION);
    for(int i=0; i<subs.length ;i++){
      subs[i].update();
    }      
    }catch(Exception e){
      printLog("Reason: " + e);
      printLog("cell update failed!");
    }
  }
  public void draw(){
    //Debug
//    for(int i=0; i<subs.length;i++){
//      if(subs[i]==subs[subs.length-1])
//        subs[i].draw();
//      subs[i].draw();
//    }        
                  
    strokeWeight(1);
    noFill();
    if(egg)
      stroke(eggColor);
    else    
      stroke(255,160);
    ellipse(m.p, radius / 2);
            
    noFill();
    if(!smallCell)
      strokeWeight(2);
    else
      strokeWeight(1);

    stroke(cellColor);
    
    //ellipse(historyPos,15);
    
/*
    if(isSelected)
      stroke(245,90,138);
    else
    if(hoveringOver)
      stroke(247,192,215); 
    else
    if(mouseOverMagnet)
      stroke(cellColor);
    else
      stroke(255,200);
*/      
    //fill(0);
    
    if(!smallCell){
      beginShape();  
      for(int i=0; i<subs.length-1; i++){
        curveVertex(subs[i].pos.x,subs[i].pos.y);         
      }
      curveVertex(subs[subs.length-1].pos.x,subs[subs.length-1].pos.y);     
      curveVertex(subs[0].pos.x,subs[0].pos.y);         
      curveVertex(subs[1].pos.x,subs[1].pos.y);      
      curveVertex(subs[2].pos.x,subs[2].pos.y);        
      endShape();  
    }
    else{      
      
      if(!egg)
        ellipse(m.p,radius);
      else
        ellipse(m.p, radius + 5 * sin(sinWave));
    }       
    
    if(user!=null){
      if(user.gearonUser){
        drawGearonIndicator();
      }
    }
    
    /*
    if(hoveringOver){
      pushMatrix();
      translate(m.p.x+radius*2,m.p.y+6);
      fill(cellColor);
      textAlign(LEFT);      
      textFont(guiFont,12);
      text(user.handle,0,0);
      popMatrix();   
    } */
    
    drawHistoryLine();
       
    drawFriendLinks();    
  }
  
  public void useHistoryLine(){
    inHistoryLog = true;
    cellHistoryLineOpa = 180;
  }
  
  public void drawHistoryLine(){
    if(inHistoryLog){
      
      if(cellHistoryLineOpa <=5){
        inHistoryLog = false;        
        return;
      }
      
      float sx = m.p.x + (historyPos.x - m.p.x) * .1f;
      float sy = historyPos.y;
      historyAnchor.nowEquals(sx,sy);
      
      smooth();
      stroke(255,cellHistoryLineOpa);
      strokeWeight(1);
      noFill();
      
      curveTightness(.95f);      
      beginShape();
      curveVertex(m.p);      
      curveVertex(m.p);
      curveVertex(historyAnchor);
      curveVertex(historyPos);
      curveVertex(historyPos);      
      endShape();
      curveTightness(-1);        
//      line(m.p, historyAnchor);
//      line(historyAnchor, historyPos);
      
      inHistoryLog = false;
    }
  }
  
  public void  drawGearonIndicator(){
    if(!user.gearonUser)
      return;
    if(radius < 5)
      return;
    spinnerAngle+=2;
    pushMatrix();
    translate(m.p);
    rotate(radians(spinnerAngle));    
    pushMatrix();    
    float angle = radians((float)1/6.0f*360.0f);    
    for(int i=0;i<6;i++){
      rotate( angle );            
      strokeWeight(4);
      stroke(cellColor);
      line(radius*.5f,0,radius*.85f,0);      
    }
    popMatrix();    
    popMatrix();
  }
  
  Vec2 friendAnchor = new Vec2();
  float friendPathTimer = 0;
  Vector friendPath = new Vector();
  Vector friendPathTime = new Vector();
  float friendLinkOpa = 0;
  public void drawFriendLinks(){
    if(friendLinkOpa < 1)
      return;
    
    if(user.flickrFriends.size() <= 0)
      return;                     
      
    strokeWeight(1);
    for(int i=0; i<user.flickrFriends.size(); i++){
      User friend = (User) user.flickrFriends.get(i);
      Cell friendCell = friend.cell;      
      
      float sx = m.p.x + (friendCell.m.p.x - m.p.x) * .1f;
      float sy = friendCell.m.p.y;
      friendAnchor.nowEquals(sx,sy);         
      
      gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_DST_ALPHA);                                  
      smooth();
      noFill();
      pushMatrix();
            
      stroke(0xff7EF25F,friendLinkOpa / 2);                 
      beginShape(); 
      curveVertex(m.p);
      curveVertex(m.p);      
      
      curveVertex(friendAnchor);
      
      stroke(0xffC0F08A,friendLinkOpa);
      curveVertex(friendCell.m.p);
      curveVertex(friendCell.m.p);      
      endShape();
      
      
      if(hoveringOver || isSelected){
        ellipse(friendCell.m.p,15);
      
        Float fl = (Float) friendPathTime.get(i);
        friendPathTimer = fl.floatValue();
        friendPathTimer = lerp(friendPathTimer, 1, .05f);
        if(friendPathTimer >= .998f)
          friendPathTimer = 0.01f;      
        
        fl = new Float(friendPathTimer);
        friendPathTime.set(i,fl);
      
        Vec2 follower = (Vec2) friendPath.get(i);
        follower.nowEquals(curvePoint(m.p.x, friendAnchor.x, friendCell.m.p.x, friendCell.m.p.x, friendPathTimer), 
                           curvePoint(m.p.y, friendAnchor.y, friendCell.m.p.y, friendCell.m.p.y, friendPathTimer));
        //stroke(255, ((friendPathTimer * 205) + 50) * (friendLinkOpa / 255)  );

//        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_DST_ALPHA);                                
//        gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE);
        tint(180,253,56,((friendPathTimer * 205) + 180) * (friendLinkOpa / 255)  );
        pushMatrix();
        translate(follower);
        translate(-16, -16);        
//        scale(1.4);
//        scale( ( .05 / friendPathTimer) );        
        image(tinyGlow,0,0);
        popMatrix();
        noTint();
//        ellipse(follower, (2 + .05/ friendPathTimer) * 2);
      }
      gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);            
      popMatrix();
    }
  }
}

class SubCell{
  Vec2 pos;
  float radius = 15;
  SubCell linked[];
  mass m;
  SubCell(Vec2 pos){
    m = new mass(pos);
    this.pos = new Vec2(pos);    
  }
    
  public void update(){
//    m.v.add(new Vec2(0,.1));
    m.update();
    m.dampScalar(FRICTION);
    pos.x = m.p.x;
    pos.y = m.p.y;
  }
  
  public void draw(){
    noFill();
    stroke(0,255,20);
    ellipse(pos,radius);
  }
  
  public void connectWith(SubCell c){
    m.connectSpringTo(c.m, springConstant);
  }
}

public void drawCellHistoryLines(){
  for(int i=0; i<cells.size(); i++){
    Cell cell = (Cell) cells.get(i);
    cell.drawHistoryLine();
  }
}

public void updateCells(){
  //this will give our larger ellipses a hexagonal look
  curveTightness(-1);  
  hoveringCell = null;
  mouseOverCell = false;
  for(int i=0;i<cells.size();i++){
    Cell cell = (Cell) cells.get(i);
    cell.update();
    cell.draw();    
    
    if(cell.hoveringOver){
      mouseOverCell = true;
      hoveringCell = cell;
    }
  }
  if(selectedCell == null)
    aCellSelected = false;
  else
    aCellSelected = true;


  //the below is deprecated
  //now using RDC for collision handling
  
  recursiveClustering(cells, 0, 1);  
  
//  if(mousePressed){
//    for(int i=0;i<cells.length;i++){
//      sfSingle(cells[i].m,new mass(mouseX,mouseY), 0,  cells[i].weight);//.001 + ( ( cells[i].weight) * .02));    
//    }
//  }

//  float testDistance = 10;
//  float d1=0;
//  float d2=0;
//  float d3=0;
//  float minDistance;
//  Cell cell_i;
//  Cell cell_s;
  
  
  //cell to cell collisions
  //this stands to be heavily optimized  
  /*
  for(int i=0; i<cells.size();i++){        
    cell_i = (Cell) cells.get(i);    
    for(int s=i+1; s<cells.size(); s++){
      cell_s = (Cell) cells.get(s);      
      d2 = dist(cell_i.m.p, cell_s.m.p);
      minDistance = cell_i.radius + cell_s.radius;
            if(d2 < minDistance*.8)
             sfDouble(cell_i.m, cell_s.m, minDistance*.8, .6);

      for(int a=0; a<cell_i.subs.length; a++){
        d3 = dist(cell_i.subs[a].m.p, cell_s.m.p);
        if(d3 < minDistance*.8)
          sfDouble(cell_i.subs[a].m, cell_s.m, minDistance*.8, .8);

        for(int b=0; b<cell_s.subs.length; b++){
          d1 = dist(cell_i.subs[a].m.p, cell_s.subs[b].m.p);
          if(d1 < testDistance){
            sfDouble(cell_i.subs[a].m, cell_s.subs[b].m, testDistance, .8);
          }
        }
      } 
    }
  }  
  
 */
}

public void selectCell(Cell cell){
  if(cell == null)
    return;
  
  //eggs can't be selected
  //saves a lot of headaches...
  //you shouldn't be touching the younglings, anyway  
  //pervert
  if(cell.egg)
    return;
    
  //play a sound
  if(!sound.disableSound)
    sound.itemClickSound.play();  
    
    
  Cell lastSelectedCell = selectedCell;  
  
  if(lastSelectedCell == cell)
    return;
  
  if(selectedCell != null)
    if(selectedCell.user!=null)   
        selectedCell.user.clearImages();
        
  selectedCell = cell;
  setUserToDraw(selectedCell);
  addToUserLog(selectedCell);    
        
  //onclick, begin streaming images
  if(selectedCell.user!=null)
      selectedCell.user.streamImages(); 
        
  //reset userinfo display animations
    resetUserDisplay();  
              
}

//Controls
public void mouseReleased(){     
  if(!focused)
    return;
    
  if(mouseOverArt){
    cursor(ARROW);
    return;
  }
  
  if(mouseOverQuery){
    if(draggingMagnet){
      draggingMagnet   = false;  
      draggedMagnet    = null;      
    }
  }
  else{
    draggingMagnet   = false;  
    draggedMagnet    = null;
  }

  if(draggingScreen)  
    draggingScreen   = false;
    
  if(mouseOverDraggable && !draggingDraggable){
    if(hoveringDraggable!=null){
      hoveringDraggable.using = !hoveringDraggable.using;
      
      if(hoveringDraggable == draggableMute)
        sound.toggleMute();
      if(!sound.disableSound)
        sound.itemClickSound.play();            
    }
  }    
  
  /*
  if(draggableFullScreen.mouseOver){
    if(draggableFullScreen.using)
      changeToFullScreen();
    else
      changeToWindowed();
  }*/
  
  if(draggingDraggable)
    draggingDraggable = false;
}//end mouseReleased()

public void mousePressed(){
  if(!focused)
    return;  
//  println(mouseX);
//  println(mouseY);

  //manage query mousePressed
  
  if(mouseOverArt){
    cursor(ARROW);        
    if(artLink.equals("") || artLink == null)
      return;      
    if(!sound.disableSound)      
      sound.itemClickSound.play();      
    link(artLink, "gearonAmorphic");
    return;
  }    
  
  if(queryActive && !mouseOverQuery){
    queryActive = false;
    if(!sound.disableSound)    
      sound.itemClickOff.play();
    if(queryString.length()<=0){
      queryString=initialQuery;
      queryTouched = false;     
    }         
  }  
  
  if(mouseOverField){    
    if(selectedField!=null)
      selectedField.active = false;
    if(hoveringField!=null)
      hoveringField.mousePressed();
    if(!sound.disableSound)    
      sound.itemClickSound.play();     
    return;
  }
  else{
    deselectAllFields();
  }
  
  if(mouseOverQuery && queryUsable){
    selectedCell = null;     
    if(!sound.disableSound)    
      sound.itemClickSound.play();
    queryActive = !queryActive;
    if(!queryTouched){
      queryTouched = true;
      queryString = "";
    }    
    if(queryString.length()>0)
      queryAuto = autoComplete(queryString);
    return;
  }
  
  //manage magnet mousePressed
  //deselection
  if(!draggingMagnet && !mouseOverMagnet){
    if(selectedCell != null && !mouseOverCell){
      if(selectedCell.user!=null)
        selectedCell.user.clearImages();
      selectedCell = null;    
    }
    if(!sound.disableSound)
      sound.itemClickOff.play();
  }    
  
  if(mouseOverMagnet && hoveringMagnet!=null){
    if(!hoveringMagnet.mouseOverClose){
      if(!sound.disableSound)      
        sound.itemClickSound.play();     
    }else{
      if(!sound.disableSound)      
        sound.itemClickOff.play();          
    }      
  }

  //manage cell mousePressed
  if(!draggingMagnet){
    for(int i=0;i<cells.size();i++){    
      Cell cell = (Cell)cells.get(i);
      if(cell.hoveringOver){        
        selectCell(cell);  
        break;
      }
    }      
  }
  
  
  
}//end mousePressed()

public void mouseDragged(){ 
  if(!focused)
    return;  
  if(mouseOverArt)
    return;
  if(!draggingMagnet && !draggingScreen && mouseOverDraggable){
    draggingDraggable = true;
    draggedDraggable = hoveringDraggable;
  }
}//end mouseDragged()

public void keyReleased(){
  for(int i=0; i<magnets.size(); i++){
    Magnet m = (Magnet) magnets.get(i);
    
    //delete a magnet by hovering over it and pressing delete
    if(m.mouseOver && keyCode == 127)
      magnets.remove(i);
  }
}//end keyReleased()

public void keyPressed(){
  //println(keyCode);
  
  //hit enter after we've entered a query string
  if(queryActive && keyCode == 10){    
    String testString = queryString;
    if(useAutoString)
      testString = queryAuto;
    
    //don't create a magnet if query string is empty
    if(queryString.length()<=0){
      queryString = initialQuery;
      queryActive = false;
      queryTouched = false;  
      
      //play sound on fail
      if(!sound.disableSound)      
        sound.itemClickOff.play();      
      return;
    }
    
    //don't create a magnet if same magnet already exists
    if(!doesMagnetExist(testString)){
      queryActive = false;      
      
      //success! create the magnet
      createMagnet(testString, queryCustomRadius);    
      
      //do sound for magnet creation
      if(!sound.disableSound)      
        sound.magnetCreate.play();
      
      precomputeStippleEllipse(queryCustomRadius, queryStippleAmount);
      if(useAutoString){
        queryString = queryAuto;
        useAutoString = false; 
        
        //play sound on fail
        if(!sound.disableSound)        
          sound.itemClickOff.play();          
      }
    }
  }else  
  if(queryActive){
    //backspace to delete
    if(keyCode == 8){
      if(queryString.length()>0 && !queryString.equals("") ){
        queryString = queryString.substring(0,queryString.length()-1);  
        queryAuto = autoComplete(queryString);     
        if(!sound.disableSound)        
          sound.magnetClickOff.play();   
      }
      if(queryString.length()==0)
        queryAuto = "";
    }
    else
    //omit these keys
    /*16 38 40 37 39 127*/
    if(keyCode==16 || keyCode==38 || keyCode==40 || keyCode==37 || keyCode==39 || keyCode==127){
      
      //up and down for using/not using autocomplete prompt
      if(keyCode==38 || keyCode==40){
        useAutoString = !useAutoString;
        if(!sound.disableSound)          
          sound.magnetClick.play();
      }
    }
    //any other keys added to query string
    else{
      queryString += key;
      if(!sound.disableSound)      
        sound.magnetClick.play();
      if(queryString.length()>0)
        queryAuto = autoComplete(queryString);
      else
        queryAuto = "";
    }
  }else
  
    if(selectedField!=null)
      selectedField.keyPressed();
  
  //handle user search box
  /*
  if(usingSearch){
    //pressed enter
    if(keyCode == 10){
      if(usingSearchAuto){
        //autocompleted
        usingSearchAuto = false;
        userSearchString = userSearchAutoString;
      }
      //regular search
      executeSearch(userSearchString);
      usingSearch = false;
      return;
    }
    
    //backspace to delete
    if(keyCode == 8){
      if(userSearchString.length()>0 && !userSearchString.equals("") ){
        userSearchString = userSearchString.substring(0,userSearchString.length()-1);  
        userSearchAutoString = autoCompleteSearch(userSearchString);     
        if(!sound.disableSound)        
          sound.magnetClickOff.play();   
      }
      if(userSearchString.length()==0)
        userSearchString = "";
    }
    else    
    if(keyCode==16 || keyCode==38 || keyCode==40 || keyCode==37 || keyCode==39 || keyCode==127){
      
      //up and down for using/not using autocomplete prompt
      if(keyCode==38 || keyCode==40){
        usingSearchAuto = !usingSearchAuto;
        if(!sound.disableSound)        
          sound.magnetClick.play();
      }
    }
    //any other keys added to field string
    else{
      userSearchString += key;
      if(!sound.disableSound)      
        sound.magnetClick.play();
      if(userSearchString.length()>0)
        userSearchAutoString = autoCompleteSearch(userSearchString);
      else
        userSearchAutoString = "";
    }    
  }
  */  
  
  if(key==ESC){
    queryActive = false;
    deselectAllFields();
    //usingSearch = false;
    
    //play sound on fail
    if(!sound.disableSound)    
      sound.itemClickOff.play();     
    
    if(queryString.length()<=0){
      queryString=initialQuery;
      queryTouched = false;     
    }     
    //prevent program exit!    
    key=0;
  }
  
  if(!queryActive && !usingTextField){
    if(key == 's')
      saveFrame("amorphic_ss-####.tif");
  }
  
  //if(key == 'u')
  //  displayUserLog = !displayUserLog;
}//end keyPressed()

Vector    draggables         = new Vector();         //list of all draggables
boolean   mouseOverDraggable = false;                //if mouse is over a draggable
boolean   draggingDraggable  = false;                //if a draggable is being dragged
Draggable hoveringDraggable  = null;                 //which draggable is the mouse over
Draggable draggedDraggable   = null;                 //which draggable is being dragged

Draggable draggableHistory;
Draggable draggableHelp;
Draggable draggableLog;
Draggable draggableMute;
Draggable draggableFullScreen;
Draggable draggableSearch;
Draggable draggableAddFlickr;
Draggable draggableAddLastfm;
Draggable draggableAddGearon;

public void initDraggables(){  
  draggableHistory     = addDraggable("history", userLogPos.x, userLogPos.y, 15);  
  draggableHelp        = addDraggable("help", 160,userLogPos.y, 12);  
  draggableHelp.using  = true;
  draggableLog         = addDraggable("network", 40,600, 12);      
  draggableMute        = addDraggable("mute", 160, userLogPos.y + 50, 10);         
  //draggableFullScreen  = addDraggable("full screen", 160, userLogPos.y + 100, 12);           
  draggableSearch      = addDraggable("find user",         810, 500, 15); 
  draggableSearch.using = true;
  draggableAddFlickr   = addDraggable("add Flickr user",   800, 540, 12);  
  draggableAddFlickr.using = true;  
  draggableAddLastfm   = addDraggable("add lastFM user",   790, 580, 12);    
  draggableAddLastfm.using = true;  
  draggableAddGearon   = addDraggable("add Fidgt user",    780, 620, 12);
  draggableAddGearon.using = true;  
}

public Draggable addDraggable(String name, float x, float y, float radius){
  Draggable d = new Draggable(new Vec2(x,y), name, radius);
  draggables.add(d);
  return d;
}

public void drawDraggables(){
  for(int i=0;i<draggables.size();i++){
    Draggable d = (Draggable) draggables.get(i);
    d.draw();
  }
}

public void updateDraggables(){
  if(!draggingDraggable){
    mouseOverDraggable = false;
    hoveringDraggable = null;
  }
  else{
    lerp(draggedDraggable.pos, draggedDraggable.pos,mp,.2f);    
    return;
  }
  for(int i=0;i<draggables.size();i++){
    Draggable d = (Draggable) draggables.get(i);            
    float distanceToMouse = 0;
    if(d.stuckToScreen)
      distanceToMouse = dist(d.pos.x + camPos.x, d.pos.y + camPos.y, mouseX, mouseY);
    else
      distanceToMouse = dist(d.pos,mp);    
    if(distanceToMouse < d.radius){
      mouseOverDraggable = true;
      hoveringDraggable = d;
      d.mouseOver = true;
    }
    else
      d.mouseOver = false;

  }  
}


class Draggable
{
  //visual
  public  mass      m               = null;             //physics object
  public  Vec2      pos             = null;             //position of draggable (not needed?)
  public  float     radius          = 100;              //radius of draggable object    
  private int     textColor       = color(255,85);

  //utility
  public  boolean   mouseOver       = false;            //is mouse over this draggable at all?
  public  boolean   using           = false;            //state switch for on/off

  //data
  public  String    name            = "";
  public  String    stateOff        = "-";
  public  String    stateOn         = "+";  

  Tooltip tooltip;

  boolean stuckToScreen = false;

  Draggable(Vec2 pos, String name, float radius){
    this.name        = name;
    this.m           = new mass(pos);
    this.pos         = new Vec2(pos);
    this.radius      = radius;        
  }

  public void update(){
  }

  public void draw(){
    pushMatrix();
    if(stuckToScreen){
      translate(pos.x+camPos.x,pos.y+camPos.y);
    }
    else
      translate(pos);
    noFill();
    strokeWeight(3);

    if(!mouseOver)
      stroke(255,30); 
    else
      stroke(255,100);
    ellipse(0,0,radius*2,radius*2);

    textFont(magnetFont,16);
    textAlign(CENTER);    


    if(aCellSelected || mouseOverMagnet)
      textColor = lerpColor(textColor, color(120,120), .4f);            
    else
      if(mouseOver)
        textColor = lerpColor(textColor, color(255,255), .4f);
      else
        textColor = lerpColor(textColor, color(255,180), .4f);

    fill(textColor);

    String state = "";
    if(using)
      state = stateOn;
    else
      state = stateOff;
    text(state + name + state,0,5);

    /*
    if(mouseOver){
     fill(helpColor);
     textFont(tinyguiFont,10);
     textAlign(LEFT);
     text(previewText,radius+10,5);
     }*/

    popMatrix();

  }

  public void stickToScreen(){
    stuckToScreen = true;
  }
}

static final User noSourceUser = null;

public boolean findFlickrUser(TextField field, String query){
  networkStreams.add( new StreamFlickrUser(field, query) );
  return false;
}

public boolean findLastfmUser(TextField field, String query){
  networkStreams.add( new StreamLastfmUser(field, query, true, noSourceUser) );  
  return false;
}

public boolean findFidgtUser(TextField field, String query){
  networkStreams.add( new StreamFidgtUser(field, query, true, noSourceUser) );
  return false;
}

int maxNeighbors = 8;


public User loadFLICKRuser(String handle){  
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

public User loadLASTFMuser(String handle){  
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

public User loadGOuser(String handle){  
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

public User findUserByHandle(String username){
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

public void linkFriends(){
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

/**
  Magnets
  These objects collide with each other and attract cells.
**/

Vector    magnets            = new Vector();         //list of magnets
float     magnetRadius       = 40;                  //default magnet radius
boolean   mouseOverMagnet    = false;                //if mouse is over a magnet
boolean   draggingMagnet     = false;                //if a magnet is being dragged
Magnet    hoveringMagnet     = null;                 //which magnet is the mouse over
Magnet    draggedMagnet      = null;                 //which magnet is being dragged
Vec2      closeButtonOffset  = new Vec2( 26, -26 );  //how far the close button is from magnet
float     closeButtonSize    = 25;                   //how big is the close button size?

//list of colors magnets are randomized to get
int     magnetColors[]   = {0xffF5607C, 0xffF7B4F6, 0xffB5C5D8, 
                              0xffA558FF, 0xff97DE62, 0xffEDBCA3,
                              0xffACFF64, 0xff006589, 0xffFAD9C0,
                              0xff81116E, 0xff36B0BF, 0xff2AD897};

class Magnet extends Draggable
{
  //visual
  public  int     magnetColor     = 0;                //magnet color coding
  private PImage    glowbg          = null;             //pointer to shadow image
  public  Vec2      closePos        = null;             //position for magnet "close button"  
  public  float     customRadius    = magnetRadius;     //radius start off as default but grow in size based on how many cells attracted
  private float     closePosRatio   = 1;                //used for calculating close button position
  
  //utility
  public  String    tagName         = "";               //what tags the magnet will attract  
  public  Hashtable cellsToAttract  = new Hashtable();  //a list of cells that are attracted to this magnet  
  public  boolean   mouseOver       = false;            //is mouse over this magnet at all (including buttons)?  
  public  boolean   mouseOverBody   = false;            //is mouse over this magnet's main body?
  public  boolean   mouseOverClose  = false;            //is mouse over the cloes button?  
  
  //animation
  public  float     sinWave         = random(100);      //used for animating back/forth cell animation
  public  float     magnitude       = 1;                //how strongly the magnet will pull

  Magnet(Vec2 pos, String tagName, float radius){
    super(pos,tagName,radius);
    //set properties
    this.tagName     = tagName;
    this.glowbg      = glow;
    
    //give magnet a random color
    int randomColor  = (int) random(magnetColors.length);
    magnetColor = magnetColors[randomColor];

    //TODO:
    //  there needs to be something in cell to re-register with existing magnets
    
    //look for cells attracted to this magnet and put it in its table
    //TODO:
    //  use hashtable as lookup for spring attraction
    //  right now this is used for color-coding only    
    for(int i=0;i<cells.size();i++){ 
      Cell cell = (Cell) cells.get(i);     
      float weightForTaste = cell.user.taste.weightFor( tagName );
      if( weightForTaste != -1 ){
        CellAttraction ca = new CellAttraction(this, (radius - cell.radius) * .8f, weightForTaste / 50 * magnitude);
        cellsToAttract.put(cell, ca );
        customRadius+=2;
      }
    }  
    
    this.radius = customRadius;  
    
    //now that we've established radius, set the closebutton position properly
    this.closePosRatio = customRadius/radius;
    this.closePos  = new Vec2(pos.x + closeButtonOffset.x * closePosRatio, pos.y + closeButtonOffset.y * closePosRatio);
    
  }//end constructor  
   
    
  public void update(){
    
    //reset sinwave if number gets way to big    
    sinWave+=.1f;    
    if(sinWave >= 100000)
      sinWave = 0;
      
    //use sinwave to modulate magnet strength to give cell wander animation      
    magnitude = 1.5f + sin(sinWave / 20);
          
    //update physics
    m.update();
    m.dampScalar(FRICTION);
    
    //reset pos
    //  is this even nessecary?
    pos.nowEquals(m.p);
    
    //update button positions...
    this.closePos = new Vec2(pos.x + closeButtonOffset.x * closePosRatio, pos.y + closeButtonOffset.y * closePosRatio);
    
  }//end update()
  
  
  public void draw(){    
    //draw shadow
    noSmooth();        
    image(glow,pos.x - glowSize ,pos.y - glowSize);
    smooth();
    
    //draw magnet ellipse
    noFill();
    strokeWeight(4);
    if(mouseOverBody && !mouseOverClose){
      fill(0,30);
      stroke(magnetColor,140);    
    }
    else{
      stroke(magnetColor,80);
    }
    ellipse(pos,radius);
    
    
    //draw buttons
    //only draw them when mouseovering this magnet    
    strokeWeight(2);
    if(mouseOver){
      if(mouseOverClose){

        //draw the close tooltip        
        noSmooth();
        fill(250);
        noStroke();
        textFont(tinyguiFont, 10);
        textAlign(LEFT);
        text("remove", closePos.x + 15, closePos.y + 2);
        
        smooth();
        fill(0,50);
        stroke(magnetColor,140);            
      }
      else{
        fill(0,50);        
        stroke(magnetColor,80);
      }    
      ellipse(closePos, closeButtonSize);    
      fill(255);
      textAlign(CENTER);
      textFont(magnetFont,16);
      text("x",closePos.x,closePos.y+5);
      //closeButtonGraphic.draw(closePos.x, closePos.y);
    }
    
    //draw magnet tag text
    textFont(magnetFont,16);
    if(!mouseOver)
      fill(255,80);
    else
      fill(255);
    noStroke();
    textAlign(CENTER);
    text(tagName, pos.x, pos.y + 10);   
   
    
  }//end draw()
}


/**
  CellAttraction
  This holds cell attraction data for springs and coloration.
**/
class CellAttraction{
  public Magnet  magnet            = null;        //holds pointer to the magnet the cell is attracted to
  public float   distance          = 0;           //distance the cell shhould stay at
  public float   strength          = 0;           //strength for the attraction spring
  public int   attractionColor   = 0xff000000;     //holds the color of the magnet cell is attracted to
  
  CellAttraction(Magnet magnet, float distance, float strength){
    this.magnet            = magnet;
    this.distance          = distance;
    this.strength          = strength;   
    this.attractionColor   = magnet.magnetColor;      
  }//end constructor
}


//This handles all magnet UI stuff
public void manageMagnets(){
  
  //reset magnet ui conditions
  mouseOverMagnet    = false;    
  hoveringMagnet     = null;
  
  for(int i=0;i<magnets.size();i++){           
    Magnet m = (Magnet) magnets.get(i);    
  
    //see if mouse is inside magnet      
    float magnetToMouse = dist(mp, m.pos);
    
    //only care about magnet mouseover if we are not dragging screen
    if(magnetToMouse < m.radius/2 && !draggingScreen && !draggingDraggable){      
      m.mouseOver       = true;
      m.mouseOverBody   = true;
      mouseOverMagnet   = true;
      hoveringMagnet    = m;            
    }
    else{
      
      //because we are in magnet, we need to test for magnet's buttons too
      if(!m.mouseOverClose){
        m.mouseOver       = false;
        m.mouseOverBody   = false;
      }
    }
    
    //if we are currently in the magnet, do tests for its buttons
    if(m.mouseOver){
      //handle close button
      float closeToMouse = dist(mp, m.closePos);
      if(closeToMouse < closeButtonSize/2){
        m.mouseOverClose = true;
        m.mouseOverBody  = false;
        mouseOverMagnet   = true;        
      }
      else
        m.mouseOverClose = false;
    }
                  
    //if we are already hovering and clicking, we are dragging
    //but only if we are not already dragging another magnet or dragging the screen!
    if(m.mouseOver && m.mouseOverBody && mousePressed && !draggingMagnet && !draggingScreen && !draggingDraggable){      
      
      //this shouldn't happen but we'll make sure anyway
      draggingScreen    = false;
      
      draggedMagnet     = m;
      draggingMagnet    = true;       
      
      //we do this to make sure mouse doesn't lose focus of magnet
      mouseOverMagnet   = true;
      m.mouseOver       = true;
      m.mouseOverBody   = true;
      m.mouseOverClose  = false;
    }    
    
    //run sounds for magnets
    if(m.mouseOver){
//      if(sound.magnetHover.state == Ess.STOPPED)
      if(!sound.disableSound)
        sound.magnetHover.play();
    }
    
    //should this be here?
    if(draggedMagnet == m){      
      //smoothly move magnet to mouse position
      lerp(m.m.p , m.m.p, mp, .2f);            
      
      //why are we repeating this...  
      m.mouseOver = true;
      mouseOverMagnet = true;
      hoveringMagnet = m;
      
      //dunno if we should do this AGAIN but it seems to get rid of flickering when dragging
      m.mouseOver       = true;
      m.mouseOverBody   = true;
      m.mouseOverClose  = false;      
    }
      
    //make changes to the magnet and draw it
    m.update();
    m.draw();  
 
    //close button is pushed. destroy this magnet! 
    if(m.mouseOver && m.mouseOverClose && mousePressed && !draggingMagnet && !draggingDraggable){
      magnets.remove(m);
      if(!sound.disableSound)        
        sound.magnetClickOff.play();
    }        
      
  }    
}//end manageMagnets()


//This function handles all magnet collisions and cell attraction
public void updateMagnets(){
  
  //collide magnets with each other
  for(int i=0; i<magnets.size(); i++){    
    Magnet m1 = (Magnet) magnets.get(i);
    
    for(int s=i+1; s<magnets.size(); s++){
      Magnet m2 = (Magnet) magnets.get(s);
      
      float distanceBetweenMagnets   = dist(m1.pos, m2.pos);
      float combinedRadii            = (m1.radius + m2.radius) *.5f;
      if(distanceBetweenMagnets < combinedRadii)
        sfDoublePD(m1.m, m2.m, distanceBetweenMagnets, combinedRadii, .4f);        
    }        
  }//end magnet collision
  
  //THIS STANDS TO BE OPTIMIZED!!
  //make cells attracted to magnets  
  for(int s=0; s<cells.size(); s++){    
    Cell cell = (Cell) cells.get(s);        
    boolean attractedToAMagnet = false;    
    
    for(int i=0; i<magnets.size(); i++){
      Magnet m = (Magnet) magnets.get(i);      
      
      //check if user has a taste for this magnet's tag
      float weightForTaste = cell.user.taste.weightFor( m.tagName );
      
      //yep. user has this tag exactly
      if( weightForTaste != -1 ){     
        //TODO:
        //  come up with some better way to handle this equation?        
        sfSingle( cell.m, m.m, (m.radius - cell.radius)*.8f, weightForTaste * .02f * m.magnitude);
        attractedToAMagnet = true;
        
        cell.wandering = false;
      }
    }
    
    //okay. cell is not attarcted to this magnet. bounce off of it.
    //TODO:
    //  shouldn't we bounce off of the magnet, anyway?
    if(!attractedToAMagnet){
      cell.wandering = true;
    }
  }     
  
}//end updateMagnets()


public void createMagnet(String queryString, float queryRadius){
  Magnet m = new Magnet(queryPos, queryString, queryRadius);
  
  //send the magnet somewhere random
  m.m.v.add(random(-20,20),random(-20,20));
  magnets.add(m);
  
//  Used for random magnet generation only  
//  genreNumber++;
//  if(genreNumber >= genreTypes.length)
//    genreNumber = 0;
      
}//end createMagnet()


//this checks if a magnet already exists
//kinda sucky brute force check
//TODO:
//  this should be kept via a hashtable. makes sense...
public boolean doesMagnetExist(String queryName){
  for(int i=0;i<magnets.size();i++){
    Magnet m              = (Magnet) magnets.get(i);
    String nameOfMagnet   = m.tagName;
    if(nameOfMagnet.equals(queryName))
      return true;
  }
  return false;
}//end doesMagnetExist()

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
  public void run(){
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
  
  public void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  public void trySleep(){
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
  public void run(){
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
  
  public void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  public void trySleep(){
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
  public void run(){
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
  
  public void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();
  }
    
  public void trySleep(){
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
  public void run(){
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
  
  public void quitThisThread(){
    //stupid hack is stupid
    //this voids non-intel macbook problems
    //thread might attempt to remove itself before it's even added
    try{
      sleep(10);
    }catch(Exception e){}
    
    networkStreams.remove(this);
    stop();    
  }
  
  public void trySleep(){
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
  public void run(){    
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
  
  public void restart(){
    finished = false;
    //start();
  }
  
  public boolean loadImages(){
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
  public void clear(){
    finished = true;
//    halt = true;
  }
}

//Properly formats a URL (spaces, special characters)
public String formatURL(String text){   
  return java.net.URLEncoder.encode(text);    
}


//Network Stream log
String   streamLog[]     = {"", "", "", "", "", "", "", "", "", "" ,"" ,""};          //holds all messages from network log
int      logSpacing      = 9;                                                         //how many pixels per line in network log
boolean  showStreamLog   = true;                                                      //whether or not to draw network log

//Add a line to the network log
public void addToStreamLog(String s){
  for(int i=streamLog.length-1; i>=1; i--){
    streamLog[i] = streamLog[i-1];
  }
  streamLog[0] = s;
}

//Draw the network log
public void drawStreamLog(){
  if(!draggableLog.using)
    return;
  pushMatrix();
  translate(draggableLog.pos);
  translate(-30,35);
  noSmooth();
  noStroke();
  fill(0xff99B291,90);
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

public void printLog(String s){
  addToStreamLog(s);
  //println(s);
}

int numberOfOrbiters = 6;
Vec2 orbiterPos[];
float orbiterSpeed = -3;
float orbiterRotation = 0;
float orbiterDistance = 40;
float orbiterAngleDev = 360 / numberOfOrbiters;
float orbiterSize = 6;
Vec2 orbiterTemp;
public void initOrbiters(){
  orbiterPos = new Vec2[numberOfOrbiters];  
  float ang = 0;
  Vec2 startingPoint = new Vec2();
  for(int i=0;i<numberOfOrbiters;i++){
    Vec2 np = newVec(startingPoint, ang, orbiterDistance);
    orbiterPos[i] = new Vec2(np);
    ang+=orbiterAngleDev;
  }
}
public void updateOrbiters(Cell selected){
  if(selected == null)
    return;
    
  orbiterDistance = selected.radius + 5;  
  float ang = orbiterRotation;
  for(int i=0;i<numberOfOrbiters;i++){
    orbiterPos[i].nowEquals(0,0);
    orbiterPos[i].disp(ang, orbiterDistance);
    ang+= orbiterAngleDev;
  }  
  orbiterRotation += orbiterSpeed;
}
public void drawOrbiters(Cell selected){
  
  if(selected == null)
    return;

  pushMatrix();
  translate(selected.m.p);  
  noStroke();
  fill(247,192,215);
  for(int i=0;i<numberOfOrbiters;i++){
    ellipse(orbiterPos[i],orbiterSize);    
  }
  popMatrix();
}

//2D Physics Library
//by Michael Chang, UCLA
//http://users.design.ucla.edu/~mflux
//May 21, 2005

//  I won't care to explain my physics here, but please feel free to poke around.
//  It's not difficult.

Vector masses = new Vector();

boolean WORLDBORDER=false;
float WORLDBORDERBUFFER=-30;
float FRICTION=0.90f;
Vec2 GRAVITY=new Vec2(0,.4f);

boolean DEBUGSPRINGS = false;
class mass
{
  Vec2 p;        //spatial position
  Vec2 v;        //velocity Vec2tor
  Vec2 a;        //acceleration Vec2tor
  mass cm[];
  float sl[];
  float sk[];
  public float x()
  {
    return p.x;
  }
  public float y()
  {
    return p.y;
  }
  mass()
  {
    p=new Vec2();
    v=new Vec2();
    a=new Vec2();
    masses.add(this);
  }
  mass(mass m)
  {
    this.p=new Vec2(m.p);
    this.v=new Vec2(m.v);
    this.a=new Vec2(m.a);
    if(cm==null)
      return;
    cm=new mass[m.cm.length];
    for(int i=0;i<cm.length;i++)
    {
      cm[i]=m.cm[i];
    }
//    System.arraycopy(m.cm,0,cm,0,cm.length);
    sl=new float[m.sl.length];
    System.arraycopy(m.sl,0,sl,0,sl.length);    
    sk=new float[m.sk.length];
    System.arraycopy(m.sk,0,sk,0,sk.length);     
    masses.add(this);    
  }
  mass(float x,float y)
  {
    p=new Vec2(x,y);
    v=new Vec2();
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p)
  {
    this.p=new Vec2(p);
    v=new Vec2();
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p,Vec2 v)
  {
    this.p=new Vec2(p);
    this.v=new Vec2(v);
    a=new Vec2();
    masses.add(this);    
  }
  mass(Vec2 p,Vec2 v,Vec2 a)
  {
    this.p=new Vec2(p);
    this.v=new Vec2(v);
    this.a=new Vec2(a);
    masses.add(this);    
  }
  public void update()
  {
//    v.add(a);
    p.add(v);
    if(WORLDBORDER)
      borders(WORLDBORDERBUFFER);
      
    if(cm!=null)
      updateSprings();
  }
  public void dampScalar(float k)
  {
    v.mul(k);
  }
  public void borders(float buff)
  {
    if(p.x>width+buff)
    {
      p.x=width+buff;      
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.x<-buff)
    {
      p.x=-buff; 
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.y>height+buff)
    {
      p.y=height+buff;
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
    if(p.y<-buff)
    {
      p.y=-buff;      
//      Vec2 nextV=new Vec2(p);
//      nextV.add(v);
//      sfSingle(this,new mass(nextV),0,-.5);
    }
  }

  public void connectSpringTo(mass m,float springLength,float springConstant)
  {
    if(m==null)
      return;
    if(cm==null)
      cm=new mass[0];
    mass temp[]=new mass[cm.length+1];
    System.arraycopy(cm,0,temp,0,cm.length);
    temp[cm.length]=m;
    cm=temp;
    if(sl==null)
      this.sl=new float[0];
    this.sl=append(sl,springLength);
    if(sk==null)
      this.sk=new float[0];
    this.sk=append(sk,springConstant);
  }
  public void connectSpringTo(mass m,float k)
  {  
    float d=dist(this.p,m.p);
    connectSpringTo(m,d,k);
  }  
  public void updateSprings()
  {
    if(cm==null)
      return;      
    for(int i=0;i<cm.length;i++)    
    {
       try{
         sfDouble(this,cm[i],sl[i],sk[i]);
       }
       catch(Exception e){
         printLog("total springs " + cm.length);
         printLog("reason: " + e);
         printLog("spring failed");         
       }
       
       if(DEBUGSPRINGS){
         strokeWeight(1);
         noFill();
         stroke(0,90,255,80);
         line(this.p, cm[i].p);
       }
    }
  }
  public void clearSprings()
  {
    cm=null;
    sk=null;
    sl=null;
  }
}
public Vec2 centerOfMass(mass m[])
{
  if(m==null||m.length==0)
    return null;
  Vec2 v=new Vec2();
  for(int i=0;i<m.length;i++)
  {
    v.add(m[i].p);
  }
  v.div(PApplet.parseFloat(m.length));
  return v;
}
public mass[] rotateMasses(mass m[],Vec2 v,float ang)
{
  mass nm[]=new mass[m.length];  
  for(int i=0;i<m.length;i++)
  {
    nm[i]=new mass(m[i]);
    nm[i].p.rotate(v,ang);
  }
  return nm;
}

//November:
//Optimizations to this piece of code gave 3x performance!!
//Basically... not calling new() every call to springForce but rather
//just reusing the same memory-space.
Vec2 displace = new Vec2();
Vec2 sf = new Vec2();
public Vec2 springForce(mass a,mass b,float rl,float k)
{
  displace.nowEquals(a.p,b.p);
  displace.mul(.5f);
  float dl=displace.mag();
  if(dl==0)
    return new Vec2();
  float ratio=rl/2/dl;
  sf.nowEquals(displace.x - displace.x*ratio , displace.y - displace.y*ratio);
  sf.mul(k);
  return sf;
}

public Vec2 springForcePreDistance(mass a,mass b, float dl, float rl,float k)
{
  displace.nowEquals(a.p,b.p);
  displace.mul(.5f);
  dl /= 2;
  if(dl==0)
    return new Vec2();
  float ratio=rl/2/dl;
  sf.nowEquals(displace.x - displace.x*ratio , displace.y - displace.y*ratio);
  sf.mul(k);
  return sf;
}

public void sfSingle(mass a,mass b, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  a.v.add(springForce(a,b,rl,k));
}

public void sfSinglePD(mass a,mass b, float dl,float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  a.v.add(springForcePreDistance(a,b,dl,rl,k));
}

public void sfDouble(mass a,mass b, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  sf = springForce(a,b,rl,k);
  a.v.add(sf);
  b.v.sub(sf);  
}

public void sfDoublePD(mass a,mass b, float dl, float rl,float k)
{
//  Vec2 sf=springForce(a,b,rl,k);
  sf = springForcePreDistance(a,b, dl, rl,k);
  a.v.add(sf);
  b.v.sub(sf);  
}

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
public String autoComplete(String query){
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

public void updateQuery(){
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
      text("_", queryPos.x + textWidth(queryString) * .5f, queryPos.y + 10);            
    
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

String genreTypes[] = {"blues", "alternative", "eclectic", "electronic", "hip hop", "classical", "soundtracks", "dance", "pop", "jazz", "reggae", "emo", "indie", "rock", "ambient", "experimental", "drum and bass", "rap"};

Hashtable allTags=new Hashtable();

public void addToGlobalTags(Tag tag){
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
  
  public void formatText(){
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
  
  public void addTag(String name, float weight){
    Tag tag = new Tag(name, weight);
    tags.add(tag);  
    addToGlobalTags(tag);    
  }
  
  public void addTags(ArrayList tags){
    for(int i=0; i<tags.size(); i++){
      Tag tag = (Tag) tags.get(i);
      this.tags.add(tag);
      addToGlobalTags(tag);      
    }
  }
  
  public float weightFor(String tagName){
    for(int i=0; i<tags.size(); i++){
      Tag tag = (Tag) tags.get(i);
      if(tag.name.startsWith(tagName))
        return tag.weight;       
    }
    return -1;
  }  
  public int numberOftags(){
    return tags.size();
  }
}

Vector       kbFields             = new Vector();
TextField    hoveringField;
TextField    selectedField;
boolean      mouseOverField       = false;
boolean      usingTextField       = false;
int          defaultFieldWidth    = 80;

TextField fieldUserSearch;

public TextField createTextField(String name, String init, float x, float y, float w, float h, Draggable draggable, int executeType, boolean hasAutocomplete){
  TextField newField = new TextField(name, init, x, y, w, h);
  if(draggable!=null)
    newField.linkToDraggable(draggable);
  if(executeType != -1)
    newField.setExecution(executeType);
  if(hasAutocomplete)
    newField.setAutoComplete();
  return newField;
}

public void setupTextFields(){
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableSearch,    USERSEARCH, true);
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddFlickr, FINDFLICKRUSER, false);  
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddLastfm, FINDLASTFMUSER, false);    
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddGearon, FINDFIDGT, false);    
}

public void updateTextFields(){
  mouseOverField = false;
  hoveringField = null;
  for(int i=0; i<kbFields.size(); i++){
    TextField textField = (TextField) kbFields.get(i);
    textField.update();
    if(textField.mouseOver){
      mouseOverField = true;
      hoveringField = textField;
    }
  }
}

public void drawTextFields(){
  for(int i=0; i<kbFields.size(); i++){
    TextField textField = (TextField) kbFields.get(i);
    textField.draw();
  }  
}


public void textFieldInput(){
  if(selectedField == null)
    selectedField.keyPressed();
}

public void deselectAllFields(){
  for(int i=0; i<kbFields.size(); i++){
    TextField textField = (TextField) kbFields.get(i);
    textField.deselect();
  }    
}

class TextField{
  String     fieldName             = "";
  String     fieldString           = "";
  String     autoCompleteString    = "";
    
  boolean    active                = false;
  boolean    mouseOver             = false;
  boolean    hasAutoComplete       = false;
  boolean    activeAuto            = false;  
  int        autoCompleteType      = -1;  
  
  Draggable  draggable             = null;  
  float      fieldWidth;
  float      fieldHeight;
  float      fieldX;
  float      fieldY;
    
  TextField(String fieldName, String initialText, float x, float y, float w, float h){    
    this.fieldName    = fieldName;
    this.fieldString  = initialText;
    this.fieldX       = x;
    this.fieldY       = y;
    this.fieldWidth   = w;
    this.fieldHeight  = h;
    kbFields.add(this);
  }
  
  public void linkToDraggable(Draggable draggable){
    this.draggable = draggable;
    this.fieldX = draggable.pos.x;
    this.fieldY = draggable.pos.y;
  }
  
  //sigh
  //kinda hacked together since textfield needs to know what callback to use
  //but it isn't an autocomplete textfield!
  public void setExecution(int type){
    autoCompleteType = type;
  }
  
  public void setAutoComplete(){
    hasAutoComplete = true;    
  }
  
  public void update(){
    if(draggable!=null){
      fieldX = draggable.pos.x;
      fieldY = draggable.pos.y;
    }      
    pushMatrix();
    translate(fieldX,fieldY);
    translate(20,8);
    mouseOver = mouseOverField(fieldWidth, fieldHeight);
    popMatrix();
  }

  public void draw(){
    if(draggable!=null)
      if(!draggable.using)
        return;    

    pushMatrix();
    //position
    translate(fieldX, fieldY); 
    translate(20,8);    

    //handle search box  
    if(active)
      stroke(255,220);  
    else
      if(mouseOver)
        stroke(255,80);  
      else
        stroke(255,20);  
    drawFieldBox(fieldWidth,fieldHeight,.8f); 

    //handle text
    pushMatrix();
    translate(4,15);
    textFont(magnetFont,16); 
    textAlign(LEFT);

    if(active && !activeAuto)
      fill(228,230,250);    
    else
      fill(228,90);
    text(fieldString,0,0); 
       
    //update textfield width
    float fieldWidthPlusBuffer = defaultFieldWidth + 10;
    float currentTextWidth = textWidth(fieldString);
    fieldWidth = currentTextWidth < fieldWidthPlusBuffer ? fieldWidthPlusBuffer : currentTextWidth + 10;

    //only draw autocomplete text if textfield has autocomplete
    if(hasAutoComplete){
      if(active){
        if(activeAuto)
          fill(228,230,250);    
        else
          fill(228,90);    
        if(fieldString.length()>1)
          text(autoCompleteString+"?", 0, 20);  
      }
    }

    if(active){
      if(frameCount%10==0)
        text("_", textWidth(fieldString) * 1, 0);      
    }

    popMatrix();  

    popMatrix();
  }

  public void drawFieldBox(float w, float h, float r){
    noFill();
    strokeWeight(2);
    curveTightness(r);
    beginShape();
    curveVertex(0,0);
    curveVertex(w,0);
    curveVertex(w,h);
    curveVertex(0,h);
    curveVertex(0,0);  
    curveVertex(w,0);
    curveVertex(w,h);  
    endShape();
  }

  public boolean mouseOverField(float w, float h){
    if(mouseX > screenX(0,0))
      if(mouseY > screenY(0,0))
        if(mouseX < screenX(w,h))
          if(mouseY < screenY(w,h))
            return true;
    return false;
  }
  
  public void mousePressed(){
    if(mouseOver){
      active = true;
      selectedField = this;
    }
    else
      active = false;
  }
  
  public void keyPressed(){
    
    //don't interact with keyboard if it's not even selected
    if(!active)
      return;
    
    //pressed enter
    if(keyCode == 10){
      if(activeAuto){
        //autoCompleted
        activeAuto = false;
        fieldString = autoCompleteString;
      }
      
      boolean successfulSearch = false;
      //regular search
      successfulSearch = executeAuto(this, autoCompleteType, fieldString);
      
      //do something here to notify whether or not something is found
      
      
      active = false;
      return;
    }
    
    //backspace to delete
    if(keyCode == 8){
      if(fieldString.length()>0 && !fieldString.equals("") ){
        fieldString = fieldString.substring(0,fieldString.length()-1);  
//        autoCompleteString = autoCompleteSearch(fieldString);     
        if(!sound.disableSound)        
          sound.magnetClickOff.play();   
      }
      if(fieldString.length()==0)
        fieldString = "";
    }
    else    
    if(keyCode==16 || keyCode==38 || keyCode==40 || keyCode==37 || keyCode==39 || keyCode==127){
      
      if(hasAutoComplete){
        //up and down for using/not using autoComplete prompt
        if(keyCode==38 || keyCode==40){
          activeAuto = !activeAuto;
          if(!sound.disableSound)        
            sound.magnetClick.play();
        }
      }
      
    }
    //any other keys added to field string
    else{
      fieldString += key;
      if(!sound.disableSound)      
        sound.magnetClick.play();

      if(fieldString.length()>0)
        autoCompleteString = autoComplete(autoCompleteType, fieldString);
      else
        autoCompleteString = "";
    }        
  }
  
  public void deselect(){
    active = false;
  }
}

/**
 * Tooltips
 * These can be stored and envoked by any object to be displayed.
 **/

class Tooltip
{
  String tipString;
}

public void drawHelp(){
  if(!draggableHelp.using)
    return;   
  pushMatrix();
  translate(draggableHelp.pos);     
  drawHelpText();
  drawLegend();
  popMatrix();
}

String helpText = "";
String helpTextLines[];

public void drawHelpText(){
  smooth();
  pushMatrix();
  translate(34,0);
  textFont(tinyguiFont,10);
  textAlign(LEFT);
  fill(helpColor);
//  text(helpText,0,0);
  for(int i=0;i<helpTextLines.length;i++){
    text(helpTextLines[i],0,i*12);
  }
  popMatrix();
  smooth();
}

public void drawLegend(){  
  pushMatrix();
  translate(34,70);
  textFont(tinyguiFont,10);
  textAlign(LEFT);

  drawExampleSpinner(0,0);
  fill(255);
  text("fidg`t user",20,0);
  
  drawExampleLoading(0,20);
  fill(255);
  text("user being loaded", 20, 20);  
  popMatrix();
}

float exampleSpinner=0;
public void drawExampleSpinner(float x, float y){
  float radius = 10;
  exampleSpinner+=2;    
  pushMatrix();
  translate(x,y);
  rotate(radians(exampleSpinner));    
  pushMatrix();    
  float angle = radians((float)1/6.0f*360.0f);    
  for(int i=0;i<6;i++){
    rotate( angle );            
    strokeWeight(4);
    stroke(255);
    line(radius*.5f,0,radius*.85f,0);      
  }
  popMatrix();    
  popMatrix();
}

public void drawExampleLoading(float x,float y){
  pushMatrix();
  translate(x,y);
  stroke(0,220);
  strokeWeight(1);
  noFill();
  ellipse(0,0,6,6);
  stroke(0,180);
  ellipse(0,0,3,3);
  popMatrix();
}

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
  
  public boolean processGearOnThumbnail(){
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
  public void hasNoThumbnail(){
    thumbnail = userAnon;    
  }
  
  public void getFLICKRData(){
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
  
  public void loadFlickrArt(){
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
  
  public String flickrThumbnailURL(int farmID, int serverID, int imageID, String secretID){
    String url = "http://farm"+farmID+".static.flickr.com/"+serverID+"/"+imageID+"_"+secretID+"_t"+".jpg";
    return url;
  }
  
  public void getLASTFMData(){
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
  
  public void loadLastFMArt(){
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
          lastfmArt[i].copy(imageBuffer,0,0,imageBuffer.width,imageBuffer.height, 0 , 0 ,PApplet.parseInt(imageWidth),PApplet.parseInt(imageHeight));                        
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
  
  public String formatTitle(String text, int maxSpacing){
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
  
  public void formatText(){
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
  
  public void streamImages(){
    if(imageStreamer.started == false)
      imageStreamer.start();
    else
      imageStreamer.restart();    
  }
  
  public void clearImages(){
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
  
  
  public void setFlickrFriends(XMLElement friendsList){ 
    friendsList = friendsList.getChild(0);
    XMLElement friends[] = friendsList.getChildren();    
    
    flickrFriendsHandle = new String[friends.length];
    
    for(int s=0; s<friends.length;s++){
      flickrFriendsHandle[s] = friends[s].getAttribute("username");      
    }
  }
  
  public void setLastfmFriends(XMLElement friendsList){
    XMLElement friends[] = friendsList.getChildren();    
    
    lastfmFriendsHandle = new String[friends.length];
    
    for(int s=0; s<friends.length;s++){
      lastfmFriendsHandle[s] = friends[s].getAttribute("username");      
    }    
  }
}


public boolean userAlreadyExists(String username){
  username = username.toLowerCase();
  for(int i=0; i<users.size(); i++){
    User user = (User) users.get(i);       
    if(username.equals( user.handle==null? user.handle : user.handle.toLowerCase()) || username.equals( user.contactLASTFM==null? user.contactLASTFM : user.contactLASTFM.toLowerCase()) || username.equals( user.contactFLICKR==null? user.contactFLICKR : user.contactFLICKR.toLowerCase()))    
      return true;
  }
  return false;
}

public float valueOf(String s){
  return Float.valueOf(s).floatValue();
}


public void selectUser(User user){
  if(user==null)
    return;
  selectCell(user.cell);
}

//User Displays
//This file contains functions that are used in displaying user-content
//This file also contains user-history log.


int maxUserLog = 6;
Vector userLog = new Vector();
//Vec2 userLogPos = new Vec2(10,100);
Vec2 userLogPos = new Vec2(35,50);
boolean displayUserLog = false;

public void addToUserLog(Cell cell){
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


public void drawUserLog(){
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
public void setUserToDraw(Cell cell){
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

public void resetUserDisplay(){
  lastfmTagX = -50;
  flickrTagX = -50;      
  artSelectCornerA.nowEquals(-150 , height + 100);
  artSelectCornerB.nowEquals(   width + 150 , height + 150);  
  artSelectCornerADest.nowEquals(-150 , height + 100);
  artSelectCornerBDest.nowEquals(   width + 150 , height + 150);  
}

Vec2 userProfilePos = new Vec2(-10,300);
public void drawUserData(){   
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

    flickrTagX = lerp(flickrTagX, 50, .2f);

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
        float x1 = screenX(0,-user.lastfmArt[i].height * .5f);
        float x2 = screenX(user.lastfmArt[i].width , user.lastfmArt[i].height * .5f);
        float y1 = screenY(0,-user.lastfmArt[i].height * .5f);
        float y2 = screenY(user.lastfmArt[i].width , user.lastfmArt[i].height * .5f);

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

    lastfmTagX = lerp(lastfmTagX, 50, .15f);
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
    lerp(artSelectCornerA, artSelectCornerA, artSelectCornerADest, .20f);
    lerp(artSelectCornerB, artSelectCornerB, artSelectCornerBDest, .20f);

    smooth();
    rectMode(CORNERS);          
    stroke(245,90,138);
    noFill();  
    rect(artSelectCornerA.x - selectedArtBorder,        artSelectCornerA.y - selectedArtBorder, 
    artSelectCornerB.x + selectedArtBorder + 1 ,   artSelectCornerB.y + selectedArtBorder + 1);
  }

}


public void drawTextBacking(float x, float y,float length, float height){
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

//2D Vector Library
//Copyright (C) Michael "Flux" Chang (www.ghost-hack.com) 2006

Vec2 origin2=new Vec2();    //origin is 0,0
//--------------BEGINNING OF VECTOR CLASS OBJECT--------------
class Vec2
{
  float x;
  float y;
  Vec2()
  {//constructor with no arguments, gives you origin
    x=0;
    y=0;
  }
  Vec2(float x,float y)
  {//constructor with 2 arguments x and y
    this.x=x;
    this.y=y;
  }
  Vec2(Vec2 s)
  {//constructor with Vec as input. basically an assignment operator
    if(s==null)
      return;
    this.x=s.x;
    this.y=s.y;
  }  
  Vec2(Vec2 s,Vec2 t)
  {//constructs a Vector from the difference of two Vectors
    float dx=t.x-s.x;
    float dy=t.y-s.y;
    this.x=dx;
    this.y=dy;
  }
  Vec2(Vec2 s,Vec2 t,float k)
  {//constructs a Vector from the difference of two Vectors, then modifies it by k
    float dx=t.x-s.x;
    float dy=t.y-s.y;
    this.x=dx*k;
    this.y=dy*k;
  }

  //--------------BEGINNING OF VECTOR METHODS--------------  
  public void disp(float ang,float magnitude)
  {//displacement. will offset this Vector by an angle and distance
    ang=radians(ang);
    x+=cos(ang)*magnitude;
    y-=sin(ang)*magnitude;    
  }
  public void rotate(float ang)
  {
    Vec2 temp=new Vec2();
    temp.disp(ang()+ang,mag());
    x=temp.x;
    y=temp.y;
  }  
  public void rotate(Vec2 v,float ang)
  {
    Vec2 cen=new Vec2(v);
    Vec2 ori=new Vec2(v,this);
    ori.rotate(180+ang);
    cen.add(ori);
    this.x=cen.x;
    this.y=cen.y;
    return;
  }
  public float ang()
  {//returns the angle between this Vector and origin
    return getAng(this,origin2);
  }
  public float mag()
  {//returns the distance between this Vector and origin
    return dist(origin2,this);
  }
  //scalar operations
  public void add(float s)
  {//addition operator
    x+=s;
    y+=s;
  }
  public void sub(float s)
  {//subtraction operator
    x-=s;
    y-=s;
  }
  public void mul(float s)
  {//multiplication operator
    x*=s;
    y*=s;
  }
  public void div(float s)
  {//division operator. returns 0 when division by zero
    if(s==0)
      return;
    x/=s;
    y/=s;
  }
  public void add(float x,float y)
  {//addition operator
    this.x+=x;
    this.y+=y;
  }
  public void sub(float x,float y)
  {//subtraction operator
    this.x-=x;
    this.y-=y;
  }
  public void mul(float x,float y)
  {//multiplication operator
    this.x*=x;
    this.y*=y;
  }
  public void div(float x,float y)
  {//division operator. returns 0 when division by zero
    if(x==0||y==0)
      return;
    this.x/=x;
    this.y/=y;
  }  
  //Vector operators
  public void add(Vec2 s)
  {//addition operator
    x+=s.x;
    y+=s.y;
  }
  public void sub(Vec2 s)
  {//subtraction operator
    x-=s.x;
    y-=s.y;
  }
  public void mul(Vec2 s)
  {//multiplication operator
    x*=s.x;
    y*=s.y;
  }
  public void div(Vec2 s)
  {//division operator. returns 0 when division by zero
    if(s.x==0||s.y==0)
      return;
    x/=s.x;
    y/=s.y;
  }  
  
  public void nowEquals(float x, float y){
    this.x = x;
    this.y = y;
  }  
  public void nowEquals(Vec2 s){
    this.x = s.x;
    this.y = s.y;
  }
  public void nowEquals(Vec2 s,Vec2 t)
  {//constructs a Vector from the difference of two Vectors

    this.x=t.x-s.x;
    this.y=t.y-s.y;
  }
  
}
//--------------END OF VECTOR OBJECT--------------
//--------------ALTERNATE METHODS OF CONSTRUCTING A VECTOR--------------
public Vec2 newVec(Vec2 v,float ang,float magnitude)
{
  Vec2 temp=new Vec2(v); 
  temp.disp(ang,magnitude);
  return temp;
}

public Vec2 newVec(float ang,float magnitude)
{
  ang=radians(ang);
  float tx=cos(ang)*magnitude;
  float ty=sin(ang)*magnitude*-1;
  Vec2 temp=new Vec2(tx,ty);  
  return temp;
}
public Vec2 nv(float x,float y)
{
  return new Vec2(x,y);
}
public Vec2 nv(Vec2 a,Vec2 b)
{
  return new Vec2(a,b);
}
public Vec2 nv(Vec2 v)
{
  return new Vec2(v.x,v.y);
}

//--------------USEFUL VECTOR TOOLS--------------
//Finds the midpoint between two positions in space
public Vec2 midPoint(Vec2 a,Vec2 b)
{
  Vec2 d=new Vec2(a,b);
  d.mul(.5f);
  Vec2 dest=new Vec2(a);
  dest.add(d);
  //  d.sub(b);
  //  Vec newVec=new Vec(a);
  //  newVec.add(d);
  return dest;
}

//Finds the average position in an array of points in space
public Vec2 avg(Vec2 v[])
{
  Vec2 total=new Vec2();
  for(int i=0;i<v.length;i++)
  {
    total.add(v[i]);
  }
  total.div(v.length);
  return total;
}

//Using point a as origin, this returns the angle between a and b.
public float getAng(Vec2 a,Vec2 b)
{
  float ang=atan2(-1*(b.y-a.y),b.x-a.x);
  ang=degrees(ang);
  if(b.y>a.y)
    return ang=360+ang;
  if(b.y==a.y&&b.x>a.x)
    return ang=0;    
  if(b.y==a.y&&b.x<a.x)
    return ang=180;
  return ang;
}

//Returns the distances between two points in space
public float dist(Vec2 a,Vec2 b)
{
  if(a==null||b==null)
    return 0;
  return dist(a.x,a.y,b.x,b.y);
}

//Returns an interpolated point between two points in space, given a ratio
Vec2 lerpN = new Vec2();
public Vec2 lerp(Vec2 a,Vec2 b,float k)
{
  lerpN = new Vec2();
  lerpN.x=lerp(a.x,b.x,k);
  lerpN.y=lerp(a.y,b.y,k);
  return lerpN;
}

public void lerp(Vec2 v, Vec2 source, Vec2 target, float k){
  v.x = lerp(source.x, target.x, k);
  v.y = lerp(source.y, target.y, k);
}

//Wraps a point in space from 0 to canvas edge, given a buffer
public Vec2 wrap(Vec2 v,float buff)
{
  if(v.x<0-buff)
    v=nv(width/2,height/2);
  if(v.x>width+buff)
    v=nv(width/2,height/2);
  if(v.y<0-buff)
    v=nv(width/2,height/2);
  if(v.y>height+buff)
    v=nv(width/2,height/2);
  return v;
}

//Constrains a point in space from 0 to canvas edge, given a buffer
public Vec2 constrain(Vec2 v,float buff)
{
  Vec2 temp=new Vec2(v);
  temp.x=constrain(temp.x,-buff,width+buff);
  temp.y=constrain(temp.y,-buff,height+buff);
  return temp;
}

//Appends a Vector point at the end of a list of Vectors
public Vec2[] append(Vec2 v[],Vec2 nv)
{
  Vec2 temp[]=new Vec2[v.length+1];
  System.arraycopy(v,0,temp,0,v.length);
  temp[v.length]=new Vec2(nv);
  return temp;  
}

//A Segment is an object consisting of two points. This makes a line...
class Seg
{
  Vec2 a;
  Vec2 b;
  Seg()
  {
    a=new Vec2();
    b=new Vec2();
  }
  Seg(Vec2 a,Vec2 b)
  {
    this.a=new Vec2(a);
    this.b=new Vec2(b);
  }
  public float mag()
  {
    return dist(a,b);
  }
}

//Sedgwick's Line Intersection algorithm
//Will return 1 if p1 to p2 to p3 is found to be rotating counter clockwise
public int CCW(Vec2 p1, Vec2 p2, Vec2 p3)
{
  float dx1, dx2, dy1, dy2;
  dx1 = p2.x - p1.x; 
  dy1 = p2.y - p1.y;
  dx2 = p3.x - p1.x; 
  dy2 = p3.y - p1.y;
  if (dx1*dy2 > dy1*dx2) return +1;
  if (dx1*dy2 < dy1*dx2) return -1;
  if ((dx1*dx2 < 0) || (dy1*dy2 < 0)) return -1;
  if ((dx1*dx1+dy1*dy1) < (dx2*dx2+dy2*dy2)) 
    return +1;
  return 0;
}

//Given lines A1A2, B1B2, this returns true if they are intersecting
public boolean intersect(Vec2 a1, Vec2 a2, Vec2 b1, Vec2 b2)
{  
  return
    ((CCW(a1, a2, b1) != CCW(a1, a2, b2))
    && (CCW(b1, b2, a1) != CCW(b1, b2, a2)));
}

//Given line Segment A and B, returns true if found intersecting
public boolean intersect(Seg a,Seg b)
{  
  if(a==null||b==null)
    return false;
  Vec2 a1=a.a;
  Vec2 a2=a.b;
  Vec2 b1=b.a;
  Vec2 b2=b.b;
  return intersect(a1,a2,b1,b2);
}

//Given line Segment A and B, returns the point of intersection
public Vec2 intersectPoint(Seg a,Seg b)
{
  Vec2 n=new Vec2();
  float x1=a.a.x;
  float x2=a.b.x;
  float x3=b.a.x;
  float x4=b.b.x;
  float y1=a.a.y;
  float y2=a.b.y;
  float y3=b.a.y;
  float y4=b.b.y;
  float nd=((y4-y3)*(x2-x1))-((x4-x3)*(y2-y1));
  if(nd==0)
    return a.a;
  float mnx=(((x4-x3)*(y1-y3))-((y4-y3)*(x1-x3)))   /   nd;
  //  float mny=(((x2-x1)*(y1-y3))-((y2-y1)*(x1-x3)))   /   nd;
  float nx=x1+mnx*(x2-x1);
  float ny=y1+mnx*(y2-y1);
  n=new Vec2(nx,ny);
  return n;
}

//Given line Segment A and B, returns the point of intersection
//Alternate method
public Vec2 intersectPoint2(Seg a,Seg b)
{
  Vec2 n=new Vec2();
  float A1=a.a.x-a.b.x;
  float B1=a.a.y-a.b.y;
  float C1=A1*a.a.x+B1*a.a.y;
  float A2=b.a.x-b.b.x;
  float B2=b.a.y-b.b.y;
  float C2=A2*b.a.x+B2*b.a.y;
  float det=(A1*B2)-(A2*B1);
  if(det==0)
  {
    return new Vec2();
  }
  else
  {
    n.x=((B2*C1 - B1*C2)/det);
    n.y=((A1*C2 - A2*C1)/det);
  }
  return n;
}

//Normalizes an angle between 0 and 360
public float normalize(float a)
{
  if(a>360)
    return a%360;
  if(a<0)
    return 360+(a%360);
  return a;
}
//Wraps a number inside a range, from minimum to maximum
public float wrap(float n,float mini,float maxi)
{
  n=n%maxi;
  if(n<mini)
    return maxi-(mini-n);
  if(n>maxi)
    return mini+(n-maxi);
  return n;
}
public void ellipse(Vec2 p,float r)
{
  ellipse(p.x,p.y,r,r);
}
public void line(Vec2 a,Vec2 b)
{
  line(a.x,a.y,b.x,b.y);
}
public void println(Vec2 v)
{
  println("point: "+v.x+","+v.y);
}
public void curveVertex(Vec2 v){
  curveVertex(v.x,v.y);
}
public void curveVec3(Vec2 v)
{
  curveVertex(v.x,v.y);
}
public void bezier(Vec2 a,Vec2 b,Vec2 c,Vec2 d)
{
  bezier(a.x,a.y,b.x,b.y,c.x,c.y,d.x,d.y);
}
public Vec2 bezierPoint(Vec2 a,Vec2 b,Vec2 c,Vec2 d,float t)
{
  float nx=bezierPoint(a.x,b.x,c.x,d.x,t);
  float ny=bezierPoint(a.y,b.y,c.y,d.y,t);  
  return new Vec2(nx,ny);
}
public void translate(Vec2 v)
{
  translate(v.x,v.y);
}
public boolean inRect(Vec2 v, float left, float right, float top, float bottom){
  if(v.x>left && v.x<right && v.y>top && v.y<bottom)
    return true;
  else return false;
}

public void vertex(Vec2 v)
{
  vertex(v.x,v.y);
}
static public void main(String args[]) {   PApplet.main(new String[] { "fidgtvisual" });}}