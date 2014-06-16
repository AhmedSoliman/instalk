### Installation

The App is developed in two seperately sides, one for the back-end and the other is front-end, so it's easy to develope and run one side seperalty without the other.

To install the front-end side, we are using some automated tools to run the and build the app so make sure to install [nodejs](http://nodejs.org/), [npm](https://www.npmjs.org/) and [grunt](http://gruntjs.com/getting-started)

After you make sure that all the above tools are setup, just follow the below simple steps to build.

From inside the `/web` directory run the following

```
npm install
```

This command will install all the grunt dependencies that you can found in the `package.json` file.

```
bower install
```

This command will install al the bower vendor dependencies that can be found in the `bower.json` file.

and finally run

```
grunt serve
``` 

This will to build your application and starts a http server that can accessed at [http://localhost:9999](http://localhost:9999)

Till now you are just running the front-end code and that's fine for development, but if you want to connect to the live server you can fo this by changing the url in the `/web/app/scripts/config.coffee` file to point to the remote server like this

```
"ws://instalk.im:9000/websocket"
```