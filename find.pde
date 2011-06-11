static final User noSourceUser = null;

boolean findFlickrUser(TextField field, String query){
  networkStreams.add( new StreamFlickrUser(field, query) );
  return false;
}

boolean findLastfmUser(TextField field, String query){
  networkStreams.add( new StreamLastfmUser(field, query, true, noSourceUser) );  
  return false;
}

boolean findFidgtUser(TextField field, String query){
  networkStreams.add( new StreamFidgtUser(field, query, true, noSourceUser) );
  return false;
}
