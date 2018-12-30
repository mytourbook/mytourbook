/* no-unused-vars */

// IE11 support
import "babel-polyfill";

// roboto font
import "typeface-roboto";

// material design icons
import "material-design-icons-iconfont/dist/material-design-icons.css"; // Ensure you are using css-loader

import Vue from "vue";

// i18n
import i18n from "./nl/i18n-setup";
// import VueI18n from "vue-i18n";
// Vue.use(VueI18n);
// const i18n = new VueI18n({
//    locale: "ja",
//    messages: {
//       en: {
//          message: {
//             hello: "hello world",
//             greeting: "good morning"
//          }
//       },
//       ja: {
//          message: {
//             hello: "こんにちは、世界",
//             greeting: "おはようございます"
//          }
//       }
//    }
// });

// // ajax
// import axios from 'axios'
// import VueAxios from 'vue-axios'
// Vue.use(VueAxios, axios)

// // lodash
// import VueLodash from 'vue-lodash'
// Vue.use(VueLodash)

// vuetify
import "./plugins/vuetify";
import App from "./App.vue";

Vue.config.productionTip = false;

new Vue({
   i18n: i18n,
   render: h => h(App)
}).$mount("#app");
