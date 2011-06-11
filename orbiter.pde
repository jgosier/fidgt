int numberOfOrbiters = 6;
Vec2 orbiterPos[];
float orbiterSpeed = -3;
float orbiterRotation = 0;
float orbiterDistance = 40;
float orbiterAngleDev = 360 / numberOfOrbiters;
float orbiterSize = 6;
Vec2 orbiterTemp;
void initOrbiters(){
  orbiterPos = new Vec2[numberOfOrbiters];  
  float ang = 0;
  Vec2 startingPoint = new Vec2();
  for(int i=0;i<numberOfOrbiters;i++){
    Vec2 np = newVec(startingPoint, ang, orbiterDistance);
    orbiterPos[i] = new Vec2(np);
    ang+=orbiterAngleDev;
  }
}
void updateOrbiters(Cell selected){
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
void drawOrbiters(Cell selected){
  
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
