// import VueI18n from 'vue-i18n'
// // import messages from './messages'

// const i18n = new VueI18n({
//    locale: 'en', //getBrowserLocale(), //'en', // set locale
//    fallbackLocale: 'en',
//    // messages // set locale messages
// })

// export default i18n

import Vue from "vue";
import VueI18n from "vue-i18n";

Vue.use(VueI18n);

function getBrowserLocale() {
   debugger;
   return navigator.language || navigator.languages[0];
}


const i18n = new VueI18n({

   locale: "de",
   // locale: getBrowserLocale(),
   
   messages: {
      en: {
         message: {
            hello: "hello world",
            greeting: "good morning"
         }
      },
      de: {
         message: {
            hello: "Das ist ein Hallo",
            greeting: "Guten Morgen"
         }
      }
   }
});

export default i18n;
