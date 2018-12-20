const port = 8080; // Easy to change the port here

const path = require('path')
const webpack = require('webpack')

module.exports = {
   devServer: {
      headers: {
         "Access-Control-Allow-Origin": "\*"
      },
   },

   assetsDir: process.env.NODE_ENV === 'production'
      ? 'static'
      : `http://localhost:${port}/`,

   baseUrl: process.env.NODE_ENV === 'production'
      ? '/' // mess up assetsDir if this is blank
      : `http://localhost:${port}/`,

   // IE11 support
   transpileDependencies:[/node_modules[/\\\\]vuetify[/\\\\]/],

   lintOnSave: false
}