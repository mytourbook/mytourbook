<template>
   <DynamicScroller
      class             ="scroller"
      :items            ="allSearchResultItems"

      :min-item-height  ="22"
      :xitem-height="32"

      emitUpdate 
      @update       ="onUpdate_SearchResultList"
   >

      <template slot-scope="{ item, index, active }">
         <DynamicScrollerItem
            :item                ="item"
            :active              ="active"
            :size-dependencies   ="[
                                    item.htmlContent,
                                 ]"
            :data-index="index"
         >
            <div class="text" v-html="item.htmlContent"></div>
         </DynamicScrollerItem>
      </template>

   </DynamicScroller>
</template>
 
<script src="./SearchResultList.vue.js"></script>

<style> 

@import '../assets/search.css';

.scroller 
{
  height: 100%;
  background-color: #eee;
}
 
.user 
{
  xheight: 32%;
  padding: 0 12px;
  display: flex;
  align-items: center;
}

</style> 
