Vector       kbFields             = new Vector();
TextField    hoveringField;
TextField    selectedField;
boolean      mouseOverField       = false;
boolean      usingTextField       = false;
int          defaultFieldWidth    = 80;

TextField fieldUserSearch;

TextField createTextField(String name, String init, float x, float y, float w, float h, Draggable draggable, int executeType, boolean hasAutocomplete){
  TextField newField = new TextField(name, init, x, y, w, h);
  if(draggable!=null)
    newField.linkToDraggable(draggable);
  if(executeType != -1)
    newField.setExecution(executeType);
  if(hasAutocomplete)
    newField.setAutoComplete();
  return newField;
}

void setupTextFields(){
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableSearch,    USERSEARCH, true);
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddFlickr, FINDFLICKRUSER, false);  
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddLastfm, FINDLASTFMUSER, false);    
  createTextField("", "", 0, 0, defaultFieldWidth, 20, draggableAddGearon, FINDFIDGT, false);    
}

void updateTextFields(){
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

void drawTextFields(){
  for(int i=0; i<kbFields.size(); i++){
    TextField textField = (TextField) kbFields.get(i);
    textField.draw();
  }  
}


void textFieldInput(){
  if(selectedField == null)
    selectedField.keyPressed();
}

void deselectAllFields(){
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
  
  void linkToDraggable(Draggable draggable){
    this.draggable = draggable;
    this.fieldX = draggable.pos.x;
    this.fieldY = draggable.pos.y;
  }
  
  //sigh
  //kinda hacked together since textfield needs to know what callback to use
  //but it isn't an autocomplete textfield!
  void setExecution(int type){
    autoCompleteType = type;
  }
  
  void setAutoComplete(){
    hasAutoComplete = true;    
  }
  
  void update(){
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

  void draw(){
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
    drawFieldBox(fieldWidth,fieldHeight,.8); 

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

  void drawFieldBox(float w, float h, float r){
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

  boolean mouseOverField(float w, float h){
    if(mouseX > screenX(0,0))
      if(mouseY > screenY(0,0))
        if(mouseX < screenX(w,h))
          if(mouseY < screenY(w,h))
            return true;
    return false;
  }
  
  void mousePressed(){
    if(mouseOver){
      active = true;
      selectedField = this;
    }
    else
      active = false;
  }
  
  void keyPressed(){
    
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
  
  void deselect(){
    active = false;
  }
}
