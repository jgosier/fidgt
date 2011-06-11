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

void initDraggables(){  
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

Draggable addDraggable(String name, float x, float y, float radius){
  Draggable d = new Draggable(new Vec2(x,y), name, radius);
  draggables.add(d);
  return d;
}

void drawDraggables(){
  for(int i=0;i<draggables.size();i++){
    Draggable d = (Draggable) draggables.get(i);
    d.draw();
  }
}

void updateDraggables(){
  if(!draggingDraggable){
    mouseOverDraggable = false;
    hoveringDraggable = null;
  }
  else{
    lerp(draggedDraggable.pos, draggedDraggable.pos,mp,.2);    
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
  private color     textColor       = color(255,85);

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

  void update(){
  }

  void draw(){
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
      textColor = lerpColor(textColor, color(120,120), .4);            
    else
      if(mouseOver)
        textColor = lerpColor(textColor, color(255,255), .4);
      else
        textColor = lerpColor(textColor, color(255,180), .4);

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

  void stickToScreen(){
    stuckToScreen = true;
  }
}
