
// IE11 support
import 'babel-polyfill'

// roboto font
import 'typeface-roboto'

// material design icons
import 'material-design-icons-iconfont/dist/material-design-icons.css' // Ensure you are using css-loader

import Vue from 'vue'

// ajax
import axios from 'axios'
import VueAxios from 'vue-axios'
Vue.use(VueAxios, axios)

// lodash
import VueLodash from 'vue-lodash'
// const options = { name: 'lodash' } // customize the way you want to call it
Vue.use(VueLodash/*, options*/) // options is optional

// vuetify
import './plugins/vuetify'
import App from "./App.vue";

Vue.config.productionTip = false;

new Vue({
   render: h => h(App)
}).$mount("#app");
