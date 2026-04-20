const axios = require('axios');

class ExternalService {
  async estimateAge(name) {
    try {
      const response = await axios.get(`https://api.agify.io?name=${encodeURIComponent(name)}`);
      if (response.data && response.data.age != null) {
        return {
          name: response.data.name,
          age: response.data.age,
          count: response.data.count
        };
      }
      return { name, age: null, count: 0 };
    } catch (error) {
      console.error(`Error fetching age for ${name}: ${error.message}`);
      return { name, age: null, count: 0 };
    }
  }
}

module.exports = new ExternalService();