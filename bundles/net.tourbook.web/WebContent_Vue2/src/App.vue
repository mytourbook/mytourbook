<template>
  <div id="app">
    <input
      v-model="message"
      type="text"
    >
    <h2 class="message">{{message}}</h2>
    <h3>This is another  h3</h3>
    <h3>This is another message h3</h3>
  </div>
</template>

<script>
export default {
  data() {
    return {
      message: "Search II"
    };
  }
};
</script>

<style>
.message {
  font-size: 18px;
  font-family: "Lucida Sans", Verdana, sans-serif;
  color: blue;
}
</style>
