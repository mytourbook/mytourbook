
// IE11 support
import 'babel-polyfill'

// roboto font
import 'typeface-roboto'

// material design icons
import 'material-design-icons-iconfont/dist/material-design-icons.css' // Ensure you are using css-loader

import Vue from "vue";
import './plugins/vuetify'
import App from "./App.vue";

Vue.config.productionTip = false;

new Vue({
   render: h => h(App)
}).$mount("#app");
