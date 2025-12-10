-- Insert sample destinations in Nepal
  INSERT INTO destinations (name, description, country, district, province, latitude, longitude, type, category, tags)
  VALUES
      (
          'Pashupatinath Temple',
          'One of the most sacred Hindu temples of Lord Shiva in the world, located on the banks of the Bagmati River.',
          'Nepal',
          'Kathmandu',
          'Bagmati',
          27.7100,
          85.3486,
          'RELIGIOUS',
          'Temple',
          ARRAY['#unesco', '#hindu', '#temple', '#cultural', '#photography']
      ),
      (
          'Boudhanath Stupa',
          'One of the largest spherical stupas in Nepal and a UNESCO World Heritage Site.',
          'Nepal',
          'Kathmandu',
          'Bagmati',
          27.7214,
          85.3611,
          'RELIGIOUS',
          'Stupa',
          ARRAY['#buddhist', '#unesco', '#stupa', '#meditation', '#cultural']
      ),
      (
          'Pokhara Lakeside',
          'Beautiful lakeside city with views of Annapurna mountain range and Phewa Lake.',
          'Nepal',
          'Pokhara',
          'Gandaki',
          28.2096,
          83.9856,
          'NATURAL',
          'Lake',
          ARRAY['#lake', '#mountains', '#boating', '#relaxing', '#sunset']
      ),
      (
          'Swayambhunath Stupa',
          'Ancient religious architecture atop a hill in the Kathmandu Valley, also known as Monkey Temple.',
          'Nepal',
          'Kathmandu',
          'Bagmati',
          27.7149,
          85.2905,
          'RELIGIOUS',
          'Stupa',
          ARRAY['#buddhist', '#monkeys', '#viewpoint', '#cultural', '#sunrise']
      ),
      (
          'Annapurna Base Camp',
          'One of the most popular trekking destinations in the world with stunning mountain views.',
          'Nepal',
          'Kaski',
          'Gandaki',
          28.5271,
          83.8383,
          'ADVENTURE',
          'Trekking',
          ARRAY['#trekking', '#mountains', '#adventure', '#hiking', '#nature']
      )
  ON CONFLICT (name, district) DO NOTHING;