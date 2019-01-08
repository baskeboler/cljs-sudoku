# cljs-sudoku
a random sudoku solution generator in clojurescript. check it out at http://baskeboler.github.io/cljs-sudoku 

## Running locally

### Prerequisites

- `node.js` version >= 8 must be installed
- `java` version >= 8 must be installed

Start a local instance with `npx shadow-cljs watch app`
If port 8080 is not available the server will start on a different port as displayed on screen 
Open your browser and point to http://localhost:8089 or whichever port the server started on.

## Building a release

`npx shadow-cljs release app` 
This will build an optimized, production ready release under folder `public/`
