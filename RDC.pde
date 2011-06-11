//This uses Steve Rabin's "Recursive Dimensional Clustering"
//Read more about it in "Game Programming Gems II"
//Basic setup that this uses can be found on polygonal labs
//http://lab.polygonal.de/articles/recursive-dimensional-clustering/

//Rewritten here by Flux for Processing

static final int subdivThreshold = 4;
static final float RDCThreshold = .001;

//Brute force collision detection
//Used on entire list or simply on a small cluster
void bruteForce(ArrayList group){
  for(int i=0; i<group.size(); i++){
    for(int s=i+1; s<group.size(); s++){
      Cell cellA = (Cell) group.get(i);
      Cell cellB = (Cell) group.get(s);

      float distance = dist(cellA.m.p, cellB.m.p);
      float radiusTotal = cellA.radius + cellB.radius;
      if( distance < radiusTotal){
        if(!cellA.ignoreCollisions && !cellB.ignoreCollisions){
          sfDoublePD(cellA.m, cellB.m, distance, radiusTotal, .6);
        }
      }           
    }
  }  
}

//Segments and sorts large lists into clusters for collision routine
void recursiveClustering(ArrayList group, int axis1, int axis2){
  
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
ArrayList getOpenCloseBounds(ArrayList group, int axis){
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
float sortBy(Object object, int type){
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
void shellSort(ArrayList a, int sortType) { 
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
