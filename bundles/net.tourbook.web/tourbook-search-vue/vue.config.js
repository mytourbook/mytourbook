/*eslint-env node */
/*eslint-disable no-unused-vars */
const mtPort = 1024 // port for mytourbook web server
const vuePort = 8080 // port for the vue server

const path = require('path')
const webpack = require('webpack')

module.exports = {
   devServer: {
      // set Cross-Origin Resource Sharing (CORS)
      headers: {
         'Access-Control-Allow-Origin': '*'
      },

      proxy: {
         // forward css icons to mt server
         '^/$MT-ICON$': {
            target: `http://localhost:${mtPort}/`,
            changeOrigin: true
         }
      }
   },

   assetsDir: process.env.NODE_ENV === 'production' ? 'static' : `http://localhost:${vuePort}/`,

   baseUrl:
      process.env.NODE_ENV === 'production'
         ? '/' // mess up assetsDir if this is blank
         : `http://localhost:${vuePort}/`,

   // IE11 support
   transpileDependencies: [/node_modules[/\\\\]vuetify[/\\\\]/],

   lintOnSave: false
}
