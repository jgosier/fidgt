/**
 * Tooltips
 * These can be stored and envoked by any object to be displayed.
 **/

class Tooltip
{
  String tipString;
}

void drawHelp(){
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

void drawHelpText(){
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

void drawLegend(){  
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
void drawExampleSpinner(float x, float y){
  float radius = 10;
  exampleSpinner+=2;    
  pushMatrix();
  translate(x,y);
  rotate(radians(exampleSpinner));    
  pushMatrix();    
  float angle = radians((float)1/6.0*360.0);    
  for(int i=0;i<6;i++){
    rotate( angle );            
    strokeWeight(4);
    stroke(255);
    line(radius*.5,0,radius*.85,0);      
  }
  popMatrix();    
  popMatrix();
}

void drawExampleLoading(float x,float y){
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
