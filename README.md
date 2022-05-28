# pacman

This is a super basic Pac-Man clone designed to teach my kids Clojure, functional programming, and Quil. It is likely not how you would do it in real life, but has the following benefits:
* purely functional;
* self-contained in a single file;
* 320 lines of code, quite a bit less without comments and duplicate functions to show different possibilities.

Possible to-dos:
* Many levels are impossible to win because Pac-Man or the exits are blocked by walls.
* Ghosts move randomly instead of trying to minimize the distance between them and Pac-Man.
* Game progression: more ghosts, walls or speed as you win levels.
* etc.

## Try it live
http://quil.info/sketches/show/5892d4d1eef45f86388d98c932958adc6cbb8e5af0449e21199958897e5a66f9

## Usage

Run `lein figwheel` in your terminal. Wait for a while until you see `Successfully compiled "resources/public/js/main.js"`. Open [localhost:3449](http://localhost:3449) in your browser.

You can use this while developing your sketch. Whenever you save your source files the browser will automatically refresh everything, providing you with quick feedback. For more information about Figwheel, check the [Figwheel repository on GitHub](https://github.com/bhauman/lein-figwheel).

## Publishing your sketch

Before you publish your sketch, run `lein do clean, cljsbuild once optimized`. This will compile your code and run Google Closure Compiler with advanced optimizations. Take `resources/public/index.html` and `resources/public/js/main.js` and upload them to server of your choice.

## License

Distributed under the Unlicense (no conditions whatsoever).
