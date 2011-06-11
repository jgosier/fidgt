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
color     magnetColors[]   = {#F5607C, #F7B4F6, #B5C5D8, 
                              #A558FF, #97DE62, #EDBCA3,
                              #ACFF64, #006589, #FAD9C0,
                              #81116E, #36B0BF, #2AD897};

class Magnet extends Draggable
{
  //visual
  public  color     magnetColor     = 0;                //magnet color coding
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
        CellAttraction ca = new CellAttraction(this, (radius - cell.radius) * .8, weightForTaste / 50 * magnitude);
        cellsToAttract.put(cell, ca );
        customRadius+=2;
      }
    }  
    
    this.radius = customRadius;  
    
    //now that we've established radius, set the closebutton position properly
    this.closePosRatio = customRadius/radius;
    this.closePos  = new Vec2(pos.x + closeButtonOffset.x * closePosRatio, pos.y + closeButtonOffset.y * closePosRatio);
    
  }//end constructor  
   
    
  void update(){
    
    //reset sinwave if number gets way to big    
    sinWave+=.1;    
    if(sinWave >= 100000)
      sinWave = 0;
      
    //use sinwave to modulate magnet strength to give cell wander animation      
    magnitude = 1.5 + sin(sinWave / 20);
          
    //update physics
    m.update();
    m.dampScalar(FRICTION);
    
    //reset pos
    //  is this even nessecary?
    pos.nowEquals(m.p);
    
    //update button positions...
    this.closePos = new Vec2(pos.x + closeButtonOffset.x * closePosRatio, pos.y + closeButtonOffset.y * closePosRatio);
    
  }//end update()
  
  
  void draw(){    
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
  public color   attractionColor   = #000000;     //holds the color of the magnet cell is attracted to
  
  CellAttraction(Magnet magnet, float distance, float strength){
    this.magnet            = magnet;
    this.distance          = distance;
    this.strength          = strength;   
    this.attractionColor   = magnet.magnetColor;      
  }//end constructor
}


//This handles all magnet UI stuff
void manageMagnets(){
  
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
      lerp(m.m.p , m.m.p, mp, .2);            
      
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
void updateMagnets(){
  
  //collide magnets with each other
  for(int i=0; i<magnets.size(); i++){    
    Magnet m1 = (Magnet) magnets.get(i);
    
    for(int s=i+1; s<magnets.size(); s++){
      Magnet m2 = (Magnet) magnets.get(s);
      
      float distanceBetweenMagnets   = dist(m1.pos, m2.pos);
      float combinedRadii            = (m1.radius + m2.radius) *.5;
      if(distanceBetweenMagnets < combinedRadii)
        sfDoublePD(m1.m, m2.m, distanceBetweenMagnets, combinedRadii, .4);        
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
        sfSingle( cell.m, m.m, (m.radius - cell.radius)*.8, weightForTaste * .02 * m.magnitude);
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


void createMagnet(String queryString, float queryRadius){
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
boolean doesMagnetExist(String queryName){
  for(int i=0;i<magnets.size();i++){
    Magnet m              = (Magnet) magnets.get(i);
    String nameOfMagnet   = m.tagName;
    if(nameOfMagnet.equals(queryName))
      return true;
  }
  return false;
}//end doesMagnetExist()
