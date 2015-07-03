var gulp = require('gulp');
var source = require('vinyl-source-stream');
var browserify = require('browserify');
var watchify = require('watchify');
var streamify = require('gulp-streamify');
var reactify = require('reactify');
var uglify = require('gulp-uglify');
var gulpif = require('gulp-if');
var gutil = require('gulp-util');
var livereload = require('gulp-livereload');


var dependencies = ['react'];

var browserifyTask = function (options) {

 /**
  * First we define our application bundler. This bundle is the
  * files you create in the "app" folder
  */
  var appBundler = browserify({
    entries: [options.src], // The entry file, normally "main.js"
    transform: [[reactify, {harmony: true}]],
    debug: options.development, // Sourcemapping
    cache: {}, packageCache: {}, fullPaths: true
  });

  /**
   * We set our dependencies as externals of our app bundler.
   * For some reason it does not work to set these in the options above
   */
  appBundler.external(options.development ? dependencies : []);

  var rebundle = function () {
    console.log('Building APP bundle');
    appBundler.bundle()
        .on('error', gutil.log)
        .pipe(source('bundle.js'))
        .pipe(gulpif(!options.development, streamify(uglify())))
        .pipe(gulp.dest(options.dest))
        // .pipe(gulpif(options.development, livereload({start: true}))); // It notifies livereload about a change if you use it
  };

  if (options.development) {
    appBundler = watchify(appBundler);
    appBundler.on('update', rebundle);
  }

  rebundle();

  if (options.development) {
    var vendorsBundler = browserify({
      debug: true, // It is nice to have sourcemapping when developing
      require: dependencies
    });

    console.log('Building VENDORS bundle');
    vendorsBundler.bundle()
        .on('error', gutil.log)
        .pipe(source('vendors.js'))
        .pipe(gulpif(!options.development, streamify(uglify())))
        .pipe(gulp.dest(options.dest))

  }
};

gulp.task('default', function () {
  browserifyTask({
    development: true,
    src: './js/main.jsx',
    dest: './dist'
  });
});

gulp.task('deploy', function (done) {
  browserifyTask({
    development: false,
    src: './js/main.jsx',
    dest: './dist'
  });
});
