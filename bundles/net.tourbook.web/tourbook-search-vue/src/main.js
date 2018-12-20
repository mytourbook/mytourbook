
// IE11 support
import 'babel-polyfill'

// roboto font
import 'typeface-roboto'

// material design icons
import "vue-material-design-icons/styles.css"

import Vue from "vue";
import './plugins/vuetify'
import App from "./App.vue";

Vue.config.productionTip = false;

new Vue({
   render: h => h(App)
}).$mount("#app");
